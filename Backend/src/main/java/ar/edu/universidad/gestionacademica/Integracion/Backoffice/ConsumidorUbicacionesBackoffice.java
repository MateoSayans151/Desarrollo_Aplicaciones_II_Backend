package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Entidades.EventoRecibido;
import ar.edu.universidad.gestionacademica.Repositorios.EventoRecibidoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.UbicacionActualizada;

/**
 * Consume backoffice.location.upserted.v1 desde la cola academic.locations.v1 que creó CORE.
 * No declara colas ni bindings. Usa ack manual y descarta eventos repetidos por eventId,
 * registrando el evento y su efecto en la misma transacción.
 */
@Component
@ConditionalOnProperty(prefix = "backoffice.events", name = "enabled", havingValue = "true")
public class ConsumidorUbicacionesBackoffice {
    static final String TIPO_EVENTO = "backoffice.location.upserted.v1";
    private static final Logger log = LoggerFactory.getLogger(ConsumidorUbicacionesBackoffice.class);

    private final SincronizacionBackofficeServicio catalogo;
    private final EventoRecibidoRepositorio inbox;
    private final TransactionTemplate transaccion;
    private final ObjectMapper json;

    public ConsumidorUbicacionesBackoffice(SincronizacionBackofficeServicio catalogo, EventoRecibidoRepositorio inbox,
                                           TransactionTemplate transaccion, ObjectMapper json) {
        this.catalogo = catalogo;
        this.inbox = inbox;
        this.transaccion = transaccion;
        this.json = json;
    }

    @RabbitListener(queues = "${backoffice.events.queue}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        long entrega = mensaje.getMessageProperties().getDeliveryTag();
        try {
            Resultado resultado = procesar(mensaje);
            canal.basicAck(entrega, false);
            if (resultado == Resultado.UBICACION_DESCONOCIDA) {
                // El evento no trae el tipo de espacio: se relee el catálogo para saber si es un aula.
                catalogo.sincronizarSinPropagar();
            }
        } catch (DataIntegrityViolationException duplicado) {
            log.info("Evento de Backoffice ya registrado por otro consumidor; se descarta");
            canal.basicAck(entrega, false);
        } catch (RuntimeException | IOException ex) {
            log.error("No se pudo procesar un evento de {}; se envía a dead letter", TIPO_EVENTO, ex);
            canal.basicNack(entrega, false, false);
        }
    }

    Resultado procesar(Message mensaje) throws IOException {
        JsonNode raiz = json.readTree(mensaje.getBody());
        String tipo = primerTexto(raiz, "type", "eventType");
        if (tipo == null) tipo = mensaje.getMessageProperties().getReceivedRoutingKey();
        if (tipo != null && !tipo.startsWith("backoffice.location.upserted")) {
            log.debug("Se ignora un evento {} en la cola de ubicaciones", tipo);
            return Resultado.IGNORADO;
        }
        String eventId = primerTexto(raiz, "eventId", "id");
        if (eventId == null) eventId = mensaje.getMessageProperties().getMessageId();
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("El evento no trae eventId");
        }
        UbicacionActualizada ubicacion = ubicacion(datos(raiz));
        if (ubicacion.locationId() == null) {
            throw new IllegalArgumentException("El evento " + eventId + " no trae locationId");
        }
        String idEvento = eventId.length() > 100 ? eventId.substring(0, 100) : eventId;
        String tipoEvento = tipo == null ? TIPO_EVENTO : tipo;
        return transaccion.execute(estado -> {
            if (inbox.existsById(idEvento)) return Resultado.DUPLICADO;
            inbox.saveAndFlush(EventoRecibido.builder().eventId(idEvento).tipo(tipoEvento)
                    .recibidoEn(Instant.now()).build());
            return catalogo.aplicarUbicacion(ubicacion) ? Resultado.APLICADO : Resultado.UBICACION_DESCONOCIDA;
        });
    }

    /** El envelope de CORE puede traer el payload en data o payload; si no, se toma la raíz. */
    private static JsonNode datos(JsonNode raiz) {
        if (raiz.hasNonNull("data") && raiz.get("data").isObject()) return raiz.get("data");
        if (raiz.hasNonNull("payload") && raiz.get("payload").isObject()) return raiz.get("payload");
        return raiz;
    }

    private static UbicacionActualizada ubicacion(JsonNode datos) {
        return new UbicacionActualizada(uuid(datos, "locationId"), uuid(datos, "siteId"),
                primerTexto(datos, "name"),
                datos.hasNonNull("capacity") ? datos.get("capacity").asInt() : null,
                primerTexto(datos, "status"));
    }

    private static UUID uuid(JsonNode nodo, String campo) {
        String valor = primerTexto(nodo, campo);
        return valor == null ? null : UUID.fromString(valor);
    }

    private static String primerTexto(JsonNode nodo, String... campos) {
        for (String campo : campos) {
            if (nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()) return nodo.get(campo).asText();
        }
        return null;
    }

    enum Resultado { APLICADO, UBICACION_DESCONOCIDA, DUPLICADO, IGNORADO }
}

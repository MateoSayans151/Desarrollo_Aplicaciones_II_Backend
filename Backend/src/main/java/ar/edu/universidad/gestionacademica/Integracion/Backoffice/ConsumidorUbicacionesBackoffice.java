package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion;
import ar.edu.universidad.gestionacademica.Integracion.Core.BandejaEventosCore;
import ar.edu.universidad.gestionacademica.Integracion.Core.EventoEntrante;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.UbicacionActualizada;
import static ar.edu.universidad.gestionacademica.Integracion.Core.EventoEntrante.texto;

/**
 * Consume backoffice.location.upserted.v1 desde la cola academic.locations.v1 que creó CORE.
 * No declara colas ni bindings (el usuario de RabbitMQ no puede). Usa ack manual, descarta
 * eventos repetidos por eventId y, si uno falla, lo guarda con su error y hace ack igual.
 */
@Component
@ConditionalOnProperty(prefix = "backoffice.events", name = "enabled", havingValue = "true")
public class ConsumidorUbicacionesBackoffice {
    static final String TIPO_EVENTO = "backoffice.location.upserted.v1";
    private static final Logger log = LoggerFactory.getLogger(ConsumidorUbicacionesBackoffice.class);

    private final SincronizacionBackofficeServicio catalogo;
    private final BandejaEventosCore bandeja;

    public ConsumidorUbicacionesBackoffice(SincronizacionBackofficeServicio catalogo, BandejaEventosCore bandeja) {
        this.catalogo = catalogo;
        this.bandeja = bandeja;
    }

    @RabbitListener(queues = "${backoffice.events.queue}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        Resultado resultado;
        try {
            resultado = procesar(mensaje);
        } catch (DataIntegrityViolationException duplicado) {
            log.info("Evento de Backoffice ya registrado por otro consumidor; se descarta");
            resultado = Resultado.DUPLICADO;
        } catch (RuntimeException | IOException ex) {
            log.error("No se pudo procesar un evento de {}; queda guardado para reprocesarlo", TIPO_EVENTO, ex);
            bandeja.registrarFallido(mensaje, ex);
            resultado = Resultado.FALLIDO;
        }
        canal.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
        if (resultado == Resultado.UBICACION_DESCONOCIDA) {
            // Un aula nueva: se relee el catálogo para traerla con todos sus datos (código, etc.).
            catalogo.sincronizarSinPropagar();
        }
    }

    Resultado procesar(Message mensaje) throws IOException {
        EventoEntrante evento = bandeja.leer(mensaje);
        if (evento.tipo() != null && !evento.tipo().startsWith("backoffice.location.upserted")) {
            log.debug("Se ignora un evento {} en la cola de ubicaciones", evento.tipo());
            return Resultado.IGNORADO;
        }
        UbicacionActualizada ubicacion = ubicacion(evento.data());
        if (ubicacion.locationId() == null) {
            throw new IllegalArgumentException("El evento " + evento.eventId() + " no trae locationId");
        }
        return bandeja.procesarUnaVez(evento, null, () -> {
            if (catalogo.aplicarUbicacion(ubicacion)) return Resultado.APLICADO;
            // La copia local sólo tiene aulas: otro tipo de espacio que no conocemos no interesa.
            return esAula(ubicacion) ? Resultado.UBICACION_DESCONOCIDA : Resultado.IGNORADO;
        }, Resultado.DUPLICADO);
    }

    private static boolean esAula(UbicacionActualizada ubicacion) {
        return ubicacion.type() == null || SincronizacionBackofficeServicio.tipo(ubicacion.type()) == TipoUbicacion.AULA;
    }

    /** data: locationId, siteId, name, capacity y status obligatorios; type opcional. */
    private static UbicacionActualizada ubicacion(JsonNode datos) {
        return new UbicacionActualizada(uuid(datos, "locationId"), uuid(datos, "siteId"),
                texto(datos, "name"),
                datos.hasNonNull("capacity") ? datos.get("capacity").asInt() : null,
                texto(datos, "status"),
                texto(datos, "type"));
    }

    private static UUID uuid(JsonNode nodo, String campo) {
        String valor = texto(nodo, campo);
        return valor == null ? null : UUID.fromString(valor);
    }

    enum Resultado { APLICADO, UBICACION_DESCONOCIDA, DUPLICADO, IGNORADO, FALLIDO }
}

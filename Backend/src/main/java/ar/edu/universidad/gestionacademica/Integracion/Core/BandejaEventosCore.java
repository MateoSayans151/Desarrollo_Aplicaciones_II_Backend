package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Entidades.EventoRecibido;
import ar.edu.universidad.gestionacademica.Repositorios.EventoRecibidoRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Inbox de los eventos que llegan por las colas de CORE (tabla eventos_recibidos).
 * <ul>
 *   <li>Deduplica por eventId: el registro y el efecto del evento van en la misma transacción.</li>
 *   <li>Un evento que no se pudo procesar se guarda con su error y su cuerpo para reprocesarlo.
 *       CORE no reintenta ante un nack y con requeue el mensaje vuelve enseguida, así que los
 *       consumidores siempre hacen ack.</li>
 * </ul>
 */
@Component
public class BandejaEventosCore {
    private static final Logger log = LoggerFactory.getLogger(BandejaEventosCore.class);
    private static final int LARGO_ERROR = 500;

    private final EventoRecibidoRepositorio inbox;
    private final TransactionTemplate transaccion;
    private final ObjectMapper json;

    public BandejaEventosCore(EventoRecibidoRepositorio inbox, TransactionTemplate transaccion, ObjectMapper json) {
        this.inbox = inbox;
        this.transaccion = transaccion;
        this.json = json;
    }

    public EventoEntrante leer(Message mensaje) throws IOException {
        return EventoEntrante.leer(json, mensaje);
    }

    /**
     * Registra el evento y aplica su efecto en una única transacción.
     *
     * @param cuerpo     se guarda junto al evento cuando el efecto todavía no se aplica (puede ser null)
     * @param siDuplicado lo que se devuelve si el eventId ya estaba registrado
     */
    public <T> T procesarUnaVez(EventoEntrante evento, String cuerpo, Supplier<T> efecto, T siDuplicado) {
        if (evento.eventId() == null || evento.eventId().isBlank()) {
            throw new IllegalArgumentException("El evento " + evento.tipo() + " no trae eventId");
        }
        return transaccion.execute(estado -> {
            if (inbox.existsById(evento.eventId())) return siDuplicado;
            inbox.saveAndFlush(EventoRecibido.builder().eventId(evento.eventId())
                    .tipo(tipo(evento.tipo())).recibidoEn(Instant.now()).payload(cuerpo).build());
            return efecto.get();
        });
    }

    /** Guarda el mensaje fallido con su error. Nunca lanza: el consumidor tiene que poder hacer ack. */
    public void registrarFallido(Message mensaje, Exception error) {
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        try {
            EventoEntrante evento = leer(mensaje);
            if (evento.eventId() == null || evento.eventId().isBlank()) {
                log.error("Evento sin eventId descartado: {}", cuerpo);
                return;
            }
            String detalle = recortar(error.getClass().getSimpleName() + ": " + error.getMessage(), LARGO_ERROR);
            transaccion.executeWithoutResult(estado -> {
                if (inbox.existsById(evento.eventId())) return;
                inbox.saveAndFlush(EventoRecibido.builder().eventId(evento.eventId()).tipo(tipo(evento.tipo()))
                        .recibidoEn(Instant.now()).payload(cuerpo).error(detalle).build());
            });
        } catch (Exception ex) {
            log.error("No se pudo guardar el evento fallido; cuerpo: {}", cuerpo, ex);
        }
    }

    private static String tipo(String tipo) {
        return tipo == null || tipo.isBlank() ? "desconocido" : recortar(tipo, 150);
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}

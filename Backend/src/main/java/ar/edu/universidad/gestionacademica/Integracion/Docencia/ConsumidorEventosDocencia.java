package ar.edu.universidad.gestionacademica.Integracion.Docencia;

import ar.edu.universidad.gestionacademica.Integracion.Core.BandejaEventosCore;
import ar.edu.universidad.gestionacademica.Integracion.Core.EventoEntrante;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Consume la cola academic.teacher-events.v1 de CORE. La cola recibe todo teacher.#, así que
 * se filtra por eventType: sólo interesan la asistencia registrada y el resultado de examen.
 * <p>
 * Docencia todavía no publicó el contrato de {@code data} de estos eventos: por ahora se
 * guardan completos en el inbox (eventos_recibidos.payload), deduplicados por eventId, para
 * aplicarlos cuando se acuerde el formato. No declara colas ni bindings.
 */
@Component
@ConditionalOnProperty(prefix = "docencia.events", name = "enabled", havingValue = "true")
public class ConsumidorEventosDocencia {
    static final String ASISTENCIA = "teacher.attendance.recorded.v1";
    static final String RESULTADO_EXAMEN = "teacher.exam-result.published.v1";
    static final Set<String> TIPOS = Set.of(ASISTENCIA, RESULTADO_EXAMEN);
    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosDocencia.class);

    private final BandejaEventosCore bandeja;

    public ConsumidorEventosDocencia(BandejaEventosCore bandeja) {
        this.bandeja = bandeja;
    }

    @RabbitListener(queues = "${docencia.events.queue}", ackMode = "MANUAL")
    public void recibir(Message mensaje, Channel canal) throws IOException {
        try {
            procesar(mensaje);
        } catch (DataIntegrityViolationException duplicado) {
            log.info("Evento de Docencia ya registrado por otro consumidor; se descarta");
        } catch (RuntimeException | IOException ex) {
            log.error("No se pudo procesar un evento de Docencia; queda guardado para reprocesarlo", ex);
            bandeja.registrarFallido(mensaje, ex);
        }
        canal.basicAck(mensaje.getMessageProperties().getDeliveryTag(), false);
    }

    Resultado procesar(Message mensaje) throws IOException {
        EventoEntrante evento = bandeja.leer(mensaje);
        if (evento.tipo() == null || !TIPOS.contains(evento.tipo())) {
            log.debug("Se ignora el evento {} de la cola de Docencia", evento.tipo());
            return Resultado.IGNORADO;
        }
        Resultado resultado = bandeja.procesarUnaVez(evento, evento.cuerpo(), () -> Resultado.REGISTRADO,
                Resultado.DUPLICADO);
        if (resultado == Resultado.REGISTRADO) {
            log.info("Evento {} {} registrado", evento.tipo(), evento.eventId());
        }
        return resultado;
    }

    enum Resultado { REGISTRADO, DUPLICADO, IGNORADO }
}

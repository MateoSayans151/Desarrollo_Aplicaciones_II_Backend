package ar.edu.universidad.gestionacademica.Integracion.Docencia;

import ar.edu.universidad.gestionacademica.Entidades.EventoRecibido;
import ar.edu.universidad.gestionacademica.Integracion.Core.BandejaEventosCore;
import ar.edu.universidad.gestionacademica.Repositorios.EventoRecibidoRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

import static ar.edu.universidad.gestionacademica.Integracion.Docencia.ConsumidorEventosDocencia.Resultado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsumidorEventosDocenciaTest {
    private final EventoRecibidoRepositorio inbox = mock(EventoRecibidoRepositorio.class);
    private final TransactionTemplate transaccion = mock(TransactionTemplate.class);
    private final Channel canal = mock(Channel.class);
    private ConsumidorEventosDocencia consumidor;

    @BeforeEach
    void preparar() {
        consumidor = new ConsumidorEventosDocencia(new BandejaEventosCore(inbox, transaccion, new ObjectMapper()));
        when(transaccion.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    void guardaElResultadoDeExamenCompletoParaAplicarloDespues() throws Exception {
        consumidor.recibir(mensaje("""
                {"specVersion":"1.0","eventId":"evt-1","eventType":"teacher.exam-result.published.v1",
                 "source":"teacher","data":{"examId":"x"}}""", "teacher.exam-result.published.v1"), canal);

        verify(inbox).saveAndFlush(argThat((EventoRecibido evento) -> evento.getEventId().equals("evt-1")
                && evento.getTipo().equals("teacher.exam-result.published.v1")
                && evento.getPayload().contains("\"examId\"")));
        verify(canal).basicAck(7L, false);
    }

    @Test
    void filtraPorRoutingKeyLosEventosQueNoInteresan() throws Exception {
        Resultado resultado = consumidor.procesar(mensaje(
                "{\"eventId\":\"evt-2\",\"eventType\":\"teacher.class.cancelled.v1\"}", "teacher.class.cancelled.v1"));

        assertThat(resultado).isEqualTo(Resultado.IGNORADO);
        verifyNoInteractions(inbox);
    }

    @Test
    void unEventoRepetidoNoSeVuelveARegistrar() throws Exception {
        when(inbox.existsById("evt-3")).thenReturn(true);

        Resultado resultado = consumidor.procesar(mensaje(
                "{\"eventId\":\"evt-3\",\"eventType\":\"teacher.attendance.recorded.v1\"}", null));

        assertThat(resultado).isEqualTo(Resultado.DUPLICADO);
        verify(inbox, never()).saveAndFlush(any());
    }

    @Test
    void unMensajeIlegibleSeConfirmaIgual() throws Exception {
        consumidor.recibir(mensaje("no es json", "teacher.attendance.recorded.v1"), canal);

        verify(canal).basicAck(7L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    private static Message mensaje(String cuerpo, String routingKey) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(7L);
        propiedades.setReceivedRoutingKey(routingKey);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}

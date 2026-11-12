package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Entidades.EventoRecibido;
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
import java.util.UUID;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.UbicacionActualizada;
import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.ConsumidorUbicacionesBackoffice.Resultado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsumidorUbicacionesBackofficeTest {
    private static final UUID LOCATION = UUID.fromString("c41a77b2-0000-0000-0000-000000000305");
    private static final UUID SITE = UUID.fromString("3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90");

    private final SincronizacionBackofficeServicio catalogo = mock(SincronizacionBackofficeServicio.class);
    private final EventoRecibidoRepositorio inbox = mock(EventoRecibidoRepositorio.class);
    private final TransactionTemplate transaccion = mock(TransactionTemplate.class);
    private final Channel canal = mock(Channel.class);
    private ConsumidorUbicacionesBackoffice consumidor;

    @BeforeEach
    void preparar() {
        consumidor = new ConsumidorUbicacionesBackoffice(catalogo, inbox, transaccion, new ObjectMapper());
        when(transaccion.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    @Test
    void aplicaElPayloadDelEnvelopeYRegistraElEventId() throws Exception {
        when(catalogo.aplicarUbicacion(any())).thenReturn(true);

        consumidor.recibir(mensaje("""
                {"eventId":"evt-1","type":"backoffice.location.upserted.v1",
                 "data":{"locationId":"%s","siteId":"%s","name":"Aula 305","capacity":60,"status":"ACTIVE"}}
                """.formatted(LOCATION, SITE)), canal);

        verify(catalogo).aplicarUbicacion(new UbicacionActualizada(LOCATION, SITE, "Aula 305", 60, "ACTIVE"));
        verify(inbox).saveAndFlush(argThat((EventoRecibido evento) -> evento.getEventId().equals("evt-1")));
        verify(canal).basicAck(7L, false);
        verify(catalogo, never()).sincronizarSinPropagar();
    }

    @Test
    void descartaUnEventoRepetidoSinVolverAAplicarlo() throws Exception {
        when(inbox.existsById("evt-1")).thenReturn(true);

        consumidor.recibir(mensaje("""
                {"eventId":"evt-1","type":"backoffice.location.upserted.v1",
                 "data":{"locationId":"%s","siteId":"%s","name":"Aula 305","capacity":60,"status":"ACTIVE"}}
                """.formatted(LOCATION, SITE)), canal);

        verify(catalogo, never()).aplicarUbicacion(any());
        verify(canal).basicAck(7L, false);
    }

    @Test
    void anteUnaUbicacionNuevaReleeElCatalogoDespuesDelAck() throws Exception {
        when(catalogo.aplicarUbicacion(any())).thenReturn(false);

        consumidor.recibir(mensaje("""
                {"eventId":"evt-2","locationId":"%s","siteId":"%s","name":"Lab","capacity":20,"status":"ACTIVE"}
                """.formatted(LOCATION, SITE)), canal);

        var orden = inOrder(canal, catalogo);
        orden.verify(canal).basicAck(7L, false);
        orden.verify(catalogo).sincronizarSinPropagar();
    }

    @Test
    void unMensajeInvalidoVaADeadLetterSinReencolar() throws Exception {
        consumidor.recibir(mensaje("{\"type\":\"backoffice.location.upserted.v1\",\"data\":{}}"), canal);

        verify(canal).basicNack(7L, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void ignoraOtrosTiposDeEvento() throws Exception {
        Resultado resultado = consumidor.procesar(mensaje("{\"eventId\":\"x\",\"type\":\"backoffice.site.upserted.v1\"}"));

        assertThat(resultado).isEqualTo(Resultado.IGNORADO);
        verifyNoInteractions(inbox);
    }

    private static Message mensaje(String cuerpo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(7L);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}

package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

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
import java.util.UUID;
import java.util.function.Consumer;

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
    @SuppressWarnings("unchecked")
    void preparar() {
        consumidor = new ConsumidorUbicacionesBackoffice(catalogo,
                new BandejaEventosCore(inbox, transaccion, new ObjectMapper()));
        when(transaccion.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        doAnswer(inv -> {
            ((Consumer<Object>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transaccion).executeWithoutResult(any());
    }

    @Test
    void aplicaElEnvelopeDeCoreYRegistraElEventId() throws Exception {
        when(catalogo.aplicarUbicacion(any())).thenReturn(true);

        consumidor.recibir(mensaje("""
                {"specVersion":"1.0","eventId":"6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f1a23",
                 "eventType":"backoffice.location.upserted.v1","source":"backoffice",
                 "occurredAt":"2026-11-17T14:05:00.000Z","subject":"location/%1$s",
                 "correlationId":"0b7e3c1d-8f2a-4b6c-9d5e-1a2b3c4d5e6f","causationId":null,
                 "actor":{"type":"service","id":"backoffice-service"},
                 "data":{"locationId":"%1$s","siteId":"%2$s","name":"Aula 305","capacity":60,"status":"ACTIVE","type":"AULA"}}
                """.formatted(LOCATION, SITE)), canal);

        verify(catalogo).aplicarUbicacion(new UbicacionActualizada(LOCATION, SITE, "Aula 305", 60, "ACTIVE", "AULA"));
        verify(inbox).saveAndFlush(argThat((EventoRecibido evento) ->
                evento.getEventId().equals("6f1c2b9e-4d3a-4e8b-9a71-2c5d8e0f1a23") && evento.getError() == null));
        verify(canal).basicAck(7L, false);
        verify(catalogo, never()).sincronizarSinPropagar();
    }

    @Test
    void descartaUnEventoRepetidoSinVolverAAplicarlo() throws Exception {
        when(inbox.existsById("evt-1")).thenReturn(true);

        consumidor.recibir(mensaje("""
                {"eventId":"evt-1","eventType":"backoffice.location.upserted.v1",
                 "data":{"locationId":"%s","siteId":"%s","name":"Aula 305","capacity":60,"status":"ACTIVE"}}
                """.formatted(LOCATION, SITE)), canal);

        verify(catalogo, never()).aplicarUbicacion(any());
        verify(canal).basicAck(7L, false);
    }

    @Test
    void anteUnAulaNuevaReleeElCatalogoDespuesDelAck() throws Exception {
        when(catalogo.aplicarUbicacion(any())).thenReturn(false);

        consumidor.recibir(mensaje("""
                {"eventId":"evt-2","eventType":"backoffice.location.upserted.v1",
                 "data":{"locationId":"%s","siteId":"%s","name":"Aula 9","capacity":20,"status":"ACTIVE","type":"AULA"}}
                """.formatted(LOCATION, SITE)), canal);

        var orden = inOrder(canal, catalogo);
        orden.verify(canal).basicAck(7L, false);
        orden.verify(catalogo).sincronizarSinPropagar();
    }

    @Test
    void unEspacioNuevoQueNoEsAulaNoDisparaLaSincronizacion() throws Exception {
        when(catalogo.aplicarUbicacion(any())).thenReturn(false);

        Resultado resultado = consumidor.procesar(mensaje("""
                {"eventId":"evt-3","eventType":"backoffice.location.upserted.v1",
                 "data":{"locationId":"%s","siteId":"%s","name":"Lab","capacity":20,"status":"ACTIVE","type":"LABORATORIO"}}
                """.formatted(LOCATION, SITE)));

        assertThat(resultado).isEqualTo(Resultado.IGNORADO);
    }

    @Test
    void unMensajeInvalidoSeGuardaConSuErrorYSeConfirma() throws Exception {
        consumidor.recibir(mensaje("""
                {"eventId":"evt-4","eventType":"backoffice.location.upserted.v1","data":{"locationId":"LAB-201"}}
                """), canal);

        verify(inbox).saveAndFlush(argThat((EventoRecibido evento) ->
                evento.getEventId().equals("evt-4") && evento.getError() != null && evento.getPayload().contains("LAB-201")));
        verify(canal).basicAck(7L, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void ignoraOtrosTiposDeEvento() throws Exception {
        Resultado resultado = consumidor.procesar(mensaje("{\"eventId\":\"x\",\"eventType\":\"backoffice.site.upserted.v1\"}"));

        assertThat(resultado).isEqualTo(Resultado.IGNORADO);
        verifyNoInteractions(inbox);
    }

    private static Message mensaje(String cuerpo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(7L);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}

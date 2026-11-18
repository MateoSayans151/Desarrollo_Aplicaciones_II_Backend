package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Entidades.EventoPublicado;
import ar.edu.universidad.gestionacademica.Repositorios.EventoPublicadoRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class PublicacionEventosCoreTest {
    private static final String URL = "https://core.test/internal/v1/events";
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-18T12:00:00.123456Z"), ZoneOffset.UTC);
    private static final PropiedadesEventosCore ACTIVO = new PropiedadesEventosCore(true, URL, Duration.ofSeconds(15), 3);

    private final EventoPublicadoRepositorio outbox = mock(EventoPublicadoRepositorio.class);
    private final ObjectMapper json = new ObjectMapper();

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void armaElEnvelopeDeCoreYLoDejaEnElOutbox() throws Exception {
        MDC.put("correlationId", "0b7e3c1d-8f2a-4b6c-9d5e-1a2b3c4d5e6f");
        var publicador = new PublicadorEventosCore(ACTIVO, outbox, json, RELOJ);

        publicador.registrar(PublicadorEventosCore.PERIODO_DEFINIDO, "term/12",
                Map.of("termId", "12", "startDate", "2027-03-01", "endDate", "2027-07-15"));

        ArgumentCaptor<EventoPublicado> guardado = ArgumentCaptor.forClass(EventoPublicado.class);
        verify(outbox).save(guardado.capture());
        JsonNode sobre = json.readTree(guardado.getValue().getSobre());
        assertThat(sobre.get("specVersion").asText()).isEqualTo("1.0");
        assertThat(sobre.get("eventId").asText()).isEqualTo(guardado.getValue().getEventId());
        assertThat(sobre.get("eventType").asText()).isEqualTo("academic.term-defined.v1");
        assertThat(sobre.get("source").asText()).isEqualTo("academic");
        assertThat(sobre.get("occurredAt").asText()).isEqualTo("2026-11-18T12:00:00.123Z");
        assertThat(sobre.get("subject").asText()).isEqualTo("term/12");
        assertThat(sobre.get("correlationId").asText()).isEqualTo("0b7e3c1d-8f2a-4b6c-9d5e-1a2b3c4d5e6f");
        assertThat(sobre.has("causationId")).isTrue();
        assertThat(sobre.get("causationId").isNull()).isTrue();
        assertThat(sobre.get("actor").get("type").asText()).isEqualTo("service");
        assertThat(sobre.get("actor").get("id").asText()).isEqualTo("academic-service");
        assertThat(sobre.get("data").get("termId").asText()).isEqualTo("12");
        assertThat(guardado.getValue().getEstado()).isEqualTo(EventoPublicado.PENDIENTE);
    }

    @Test
    void unCorrelationIdQueNoEsUuidSeReemplaza() throws Exception {
        MDC.put("correlationId", "corr-123");
        new PublicadorEventosCore(ACTIVO, outbox, json, RELOJ).registrar("academic.term-defined.v1", "term/1", Map.of());

        ArgumentCaptor<EventoPublicado> guardado = ArgumentCaptor.forClass(EventoPublicado.class);
        verify(outbox).save(guardado.capture());
        String correlationId = json.readTree(guardado.getValue().getSobre()).get("correlationId").asText();
        assertThat(correlationId).isNotEqualTo("corr-123").hasSize(36);
    }

    @Test
    void conLaPublicacionDeshabilitadaNoGuardaNada() {
        new PublicadorEventosCore(new PropiedadesEventosCore(false, URL, null, 0), outbox, json, RELOJ)
                .registrar("academic.term-defined.v1", "term/1", Map.of());

        verifyNoInteractions(outbox);
    }

    @Test
    void enviaLosPendientesConElTokenDeServicioYLosMarcaEnviados() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-academic"))
                .andExpect(content().json("{\"eventId\":\"e-1\"}"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        EventoPublicado evento = pendiente("e-1");
        when(outbox.findTop20ByEstadoOrderByCreadoEnAsc(EventoPublicado.PENDIENTE)).thenReturn(List.of(evento));

        enviador(builder).enviarPendientes();

        assertThat(evento.getEstado()).isEqualTo(EventoPublicado.ENVIADO);
        assertThat(evento.getEnviadoEn()).isNotNull();
        servidor.verify();
    }

    @Test
    void unConflictoSeMarcaRechazadoYSigueConElProximo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.CONFLICT));
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.ACCEPTED));
        EventoPublicado conflicto = pendiente("e-1");
        EventoPublicado siguiente = pendiente("e-2");
        when(outbox.findTop20ByEstadoOrderByCreadoEnAsc(EventoPublicado.PENDIENTE)).thenReturn(List.of(conflicto, siguiente));

        enviador(builder).enviarPendientes();

        assertThat(conflicto.getEstado()).isEqualTo(EventoPublicado.RECHAZADO);
        assertThat(siguiente.getEstado()).isEqualTo(EventoPublicado.ENVIADO);
        servidor.verify();
    }

    @Test
    void unErrorDeCoreQuedaPendienteYCortaLaPasada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        EventoPublicado primero = pendiente("e-1");
        EventoPublicado segundo = pendiente("e-2");
        when(outbox.findTop20ByEstadoOrderByCreadoEnAsc(EventoPublicado.PENDIENTE)).thenReturn(List.of(primero, segundo));

        enviador(builder).enviarPendientes();

        assertThat(primero.getEstado()).isEqualTo(EventoPublicado.PENDIENTE);
        assertThat(primero.getIntentos()).isEqualTo(1);
        assertThat(primero.getUltimoError()).contains("503");
        assertThat(segundo.getIntentos()).isZero();
        servidor.verify();
    }

    private EnviadorEventosCore enviador(RestClient.Builder builder) {
        ProveedorTokenServicio tokens = mock(ProveedorTokenServicio.class);
        when(tokens.token()).thenReturn("token-academic");
        return new EnviadorEventosCore(ACTIVO, outbox, tokens, builder.build(), RELOJ);
    }

    private static EventoPublicado pendiente(String eventId) {
        return EventoPublicado.builder().eventId(eventId).tipo("academic.term-defined.v1")
                .sobre("{\"eventId\":\"" + eventId + "\"}").creadoEn(Instant.now()).build();
    }
}

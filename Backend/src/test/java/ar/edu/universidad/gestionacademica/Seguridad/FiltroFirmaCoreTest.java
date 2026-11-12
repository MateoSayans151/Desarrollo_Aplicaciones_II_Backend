package ar.edu.universidad.gestionacademica.Seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroFirmaCoreTest {
    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private final FiltroFirmaCore filtro = new FiltroFirmaCore("secreto-core", Duration.ofMinutes(5),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void aceptaUnaFirmaValidaDelGateway() throws Exception {
        String ts = String.valueOf(AHORA.getEpochSecond());
        var request = peticion(ts);
        request.addHeader(FiltroFirmaCore.FIRMA, filtro.firmar(
                "GET\n/api/v1/academica/carreras\nusuario-1\ncorr-1\n" + ts).toUpperCase());

        var cadena = new MockFilterChain();
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);

        assertThat(cadena.getRequest()).isNotNull();
    }

    @Test
    void rechazaUnaFirmaQueNoCoincide() throws Exception {
        String ts = String.valueOf(AHORA.toEpochMilli());
        var request = peticion(ts);
        request.addHeader(FiltroFirmaCore.FIRMA, filtro.firmar("GET\n/api/v1/otra-ruta\nusuario-1\ncorr-1\n" + ts));

        var respuesta = ejecutar(request);

        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getContentAsString()).contains("INVALID_GATEWAY_SIGNATURE");
    }

    @Test
    void rechazaUnaFirmaVencida() throws Exception {
        String ts = AHORA.minus(Duration.ofMinutes(10)).toString();
        var request = peticion(ts);
        request.addHeader(FiltroFirmaCore.FIRMA, filtro.firmar(
                "GET\n/api/v1/academica/carreras\nusuario-1\ncorr-1\n" + ts));

        assertThat(ejecutar(request).getStatus()).isEqualTo(401);
    }

    @Test
    void rechazaUnJwtQueNoPasoPorElGateway() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/academica/carreras");
        request.addHeader("Authorization", "Bearer token");

        assertThat(ejecutar(request).getStatus()).isEqualTo(401);
    }

    @Test
    void noActuaSobreSesionesLocalesRutasInternasNiSinSecreto() throws Exception {
        assertThat(ejecutar(new MockHttpServletRequest("GET", "/api/v1/academica/carreras")).getStatus()).isEqualTo(200);

        var interna = new MockHttpServletRequest("GET", "/internal/v1/carreras");
        interna.addHeader("Authorization", "Bearer token");
        assertThat(ejecutar(interna).getStatus()).isEqualTo(200);

        var sinSecreto = new FiltroFirmaCore("", Duration.ofMinutes(5));
        var request = new MockHttpServletRequest("GET", "/api/v1/academica/carreras");
        request.addHeader("Authorization", "Bearer token");
        var respuesta = new MockHttpServletResponse();
        sinSecreto.doFilter(request, respuesta, new MockFilterChain());
        assertThat(respuesta.getStatus()).isEqualTo(200);
    }

    private static MockHttpServletRequest peticion(String timestamp) {
        var request = new MockHttpServletRequest("GET", "/api/v1/academica/carreras");
        request.setQueryString("activa=true");
        request.addHeader("Authorization", "Bearer token");
        request.addHeader(FiltroFirmaCore.USUARIO, "usuario-1");
        request.addHeader(CorrelationId.HEADER, "corr-1");
        request.addHeader(FiltroFirmaCore.TIMESTAMP, timestamp);
        return request;
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest request) throws Exception {
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(request, respuesta, new MockFilterChain());
        return respuesta;
    }
}

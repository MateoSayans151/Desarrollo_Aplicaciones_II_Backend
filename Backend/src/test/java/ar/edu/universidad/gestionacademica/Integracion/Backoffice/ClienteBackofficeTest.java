package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException;
import ar.edu.universidad.gestionacademica.Integracion.Core.ProveedorTokenServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClienteBackofficeTest {
    private static final String BASE = "https://backoffice.test";
    private static final UUID SITE = UUID.fromString("3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90");

    private MockRestServiceServer servidor;
    private ClienteBackoffice cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        ProveedorTokenServicio tokens = mock(ProveedorTokenServicio.class);
        when(tokens.token()).thenReturn("token-servicio");
        cliente = new ClienteBackoffice(new PropiedadesBackoffice(true, BASE + "/", null, null, 100, null),
                tokens, builder.build());
    }

    @Test
    void recorreTodasLasPaginasDeSedesConElTokenDeServicio() {
        servidor.expect(requestTo(BASE + "/internal/v1/sites?limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-servicio"))
                .andExpect(header("X-Correlation-Id", "corr-1"))
                .andRespond(withSuccess("""
                        {"items":[{"siteId":"%s","code":"CEN","name":"Sede Centro","address":"Av. 1",
                                   "city":"CABA","status":"ACTIVE","locationCount":3}],
                         "nextCursor":"abc+/="}""".formatted(SITE), MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/internal/v1/sites?limit=100&cursor=abc%2B%2F%3D"))
                .andRespond(withSuccess("""
                        {"items":[{"siteId":"%s","name":"Sede Norte","status":"INACTIVE"}],
                         "nextCursor":null}""".formatted(UUID.randomUUID()), MediaType.APPLICATION_JSON));

        var sedes = cliente.listarSedes("corr-1");

        assertThat(sedes).extracting(BackofficeDto.SedeBackoffice::name).containsExactly("Sede Centro", "Sede Norte");
        assertThat(sedes.get(0).siteId()).isEqualTo(SITE);
        servidor.verify();
    }

    @Test
    void pideSoloAulasDeLaSede() {
        servidor.expect(requestTo(BASE + "/internal/v1/locations?limit=100&siteId=" + SITE + "&type=AULA"))
                .andRespond(withSuccess("""
                        {"items":[{"locationId":"c41a77b2-0000-0000-0000-000000000000","siteId":"%s",
                                   "code":"PIA1254","name":"Aula 305","type":"AULA","capacity":45,
                                   "building":null,"floor":null,"accessible":true,"eventCapable":false,
                                   "equipment":["PROYECTOR"],"status":"ACTIVE"}],
                         "nextCursor":null}""".formatted(SITE), MediaType.APPLICATION_JSON));

        var aulas = cliente.listarAulas(SITE, "corr-2");

        assertThat(aulas).singleElement().satisfies(aula -> {
            assertThat(aula.code()).isEqualTo("PIA1254");
            assertThat(aula.capacity()).isEqualTo(45);
        });
        servidor.verify();
    }

    @Test
    void informaUnTokenSinPermisoComoErrorDeIntegracion() {
        servidor.expect(requestTo(BASE + "/internal/v1/sites?limit=100"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> cliente.listarSedes("corr-3"))
                .isInstanceOf(IntegracionExternaException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("backoffice:read");
    }

    @Test
    void cortaSiBackofficeRepiteElCursor() {
        String pagina = """
                {"items":[],"nextCursor":"mismo"}""";
        servidor.expect(requestTo(BASE + "/internal/v1/sites?limit=100"))
                .andRespond(withSuccess(pagina, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(BASE + "/internal/v1/sites?limit=100&cursor=mismo"))
                .andRespond(withSuccess(pagina, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.listarSedes("corr-4"))
                .isInstanceOf(IntegracionExternaException.class)
                .hasMessageContaining("cursor");
    }
}

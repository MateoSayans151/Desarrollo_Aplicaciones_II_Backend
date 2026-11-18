package ar.edu.universidad.gestionacademica.Integracion.Core;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProveedorTokenServicioTest {
    private static final String URL = "https://core.test/internal/v1/service-auth/token";

    @Test
    void pideElTokenConJsonCamelCaseYLoReutilizaMientrasEsteVigente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(once(), requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"clientId\":\"academic-service\",\"clientSecret\":\"s3cr3t\"}", true))
                .andRespond(withSuccess("{\"accessToken\":\"eyJ.abc\",\"expiresIn\":900,\"tokenType\":\"Bearer\"}",
                        MediaType.APPLICATION_JSON));
        var proveedor = new ProveedorTokenServicio(
                new PropiedadesServicioCore(URL, "academic-service", "s3cr3t", null), builder.build(),
                Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC));

        assertThat(proveedor.token()).isEqualTo("eyJ.abc");
        assertThat(proveedor.token()).isEqualTo("eyJ.abc");
        servidor.verify();
    }

    @Test
    void despuesDeInvalidarPideUnTokenNuevo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"accessToken\":\"uno\",\"expiresIn\":900}",
                MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"accessToken\":\"dos\",\"expiresIn\":900}",
                MediaType.APPLICATION_JSON));
        var proveedor = new ProveedorTokenServicio(
                new PropiedadesServicioCore(URL, "academic-service", "s3cr3t", null), builder.build(), Clock.systemUTC());

        assertThat(proveedor.token()).isEqualTo("uno");
        proveedor.invalidar();
        assertThat(proveedor.token()).isEqualTo("dos");
        servidor.verify();
    }
}

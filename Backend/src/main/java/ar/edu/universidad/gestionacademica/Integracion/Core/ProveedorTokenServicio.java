package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Obtiene el token de servicio de academic-service para las llamadas backend a backend.
 * Con CORE_SERVICE_TOKEN usa ese token tal cual; si no, lo pide a CORE y lo reutiliza hasta
 * un minuto antes de que venza (CORE lo emite por 15 minutos).
 * <p>
 * Contrato de CORE: {@code POST /internal/v1/service-auth/token} con JSON
 * {@code { "clientId", "clientSecret" }}; responde {@code { "accessToken", "expiresIn", "tokenType" }}.
 */
@Component
public class ProveedorTokenServicio {
    private static final long MARGEN_SEGUNDOS = 60;
    private static final long VIGENCIA_POR_DEFECTO = 300;

    private final PropiedadesServicioCore propiedades;
    private final RestClient http;
    private final Clock reloj;
    private String tokenVigente;
    private Instant venceEn = Instant.EPOCH;

    @Autowired
    public ProveedorTokenServicio(PropiedadesServicioCore propiedades, RestClient.Builder builder) {
        this(propiedades, builder.requestFactory(fabricaConTimeouts()).build(), Clock.systemUTC());
    }

    ProveedorTokenServicio(PropiedadesServicioCore propiedades, RestClient http, Clock reloj) {
        this.propiedades = propiedades;
        this.http = http;
        this.reloj = reloj;
    }

    private static SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(10_000);
        return fabrica;
    }

    public synchronized String token() {
        if (tieneTexto(propiedades.token())) {
            return propiedades.token().trim();
        }
        if (!tieneTexto(propiedades.tokenUrl()) || !tieneTexto(propiedades.clientSecret())) {
            throw new IntegracionExternaException("Falta configurar el token de servicio: definí CORE_SERVICE_TOKEN "
                    + "o CORE_SERVICE_CLIENT_SECRET");
        }
        if (tokenVigente != null && reloj.instant().isBefore(venceEn)) {
            return tokenVigente;
        }
        return pedirToken();
    }

    /** Ante un 401 de otro servicio: el próximo {@link #token()} pide uno nuevo. */
    public synchronized void invalidar() {
        tokenVigente = null;
    }

    private String pedirToken() {
        Map<String, String> credenciales = Map.of(
                "clientId", propiedades.clientId(),
                "clientSecret", propiedades.clientSecret());
        Map<?, ?> respuesta;
        try {
            respuesta = http.post().uri(propiedades.tokenUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(credenciales)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            throw new IntegracionExternaException("CORE no entregó el token de servicio: " + ex.getMessage(), ex);
        }
        Object token = primero(respuesta, "accessToken", "access_token");
        if (token == null || token.toString().isBlank()) {
            throw new IntegracionExternaException("La respuesta de CORE no incluye accessToken");
        }
        long vigencia = vigencia(primero(respuesta, "expiresIn", "expires_in"));
        tokenVigente = token.toString();
        venceEn = reloj.instant().plusSeconds(Math.max(0, vigencia - MARGEN_SEGUNDOS));
        return tokenVigente;
    }

    private static Object primero(Map<?, ?> mapa, String... claves) {
        if (mapa == null) return null;
        for (String clave : claves) {
            if (mapa.get(clave) != null) return mapa.get(clave);
        }
        return null;
    }

    private static long vigencia(Object valor) {
        if (valor instanceof Number numero) return numero.longValue();
        if (valor != null) {
            try {
                return Long.parseLong(valor.toString());
            } catch (NumberFormatException ignorado) {
                // Se usa la vigencia por defecto.
            }
        }
        return VIGENCIA_POR_DEFECTO;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}

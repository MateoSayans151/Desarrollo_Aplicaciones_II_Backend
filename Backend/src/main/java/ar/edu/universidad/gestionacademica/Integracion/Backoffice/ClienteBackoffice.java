package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException;
import ar.edu.universidad.gestionacademica.Integracion.Core.ProveedorTokenServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.*;

/**
 * Lee el catálogo de Backoffice por sus rutas internas, con el token de servicio de
 * academic-service (permiso backoffice:read). Recorre todas las páginas de nextCursor.
 */
@Component
public class ClienteBackoffice {
    private static final int MAXIMO_PAGINAS = 500;
    private static final ParameterizedTypeReference<Pagina<SedeBackoffice>> PAGINA_SEDES =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Pagina<UbicacionBackoffice>> PAGINA_UBICACIONES =
            new ParameterizedTypeReference<>() { };

    private final PropiedadesBackoffice propiedades;
    private final ProveedorTokenServicio tokens;
    private final RestClient http;

    @Autowired
    public ClienteBackoffice(PropiedadesBackoffice propiedades, ProveedorTokenServicio tokens,
                             RestClient.Builder builder) {
        this(propiedades, tokens, builder.requestFactory(fabricaConTimeouts()).build());
    }

    ClienteBackoffice(PropiedadesBackoffice propiedades, ProveedorTokenServicio tokens, RestClient http) {
        this.propiedades = propiedades;
        this.tokens = tokens;
        this.http = http;
    }

    private static SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        return fabrica;
    }

    /** Todas las sedes, en cualquier estado. */
    public List<SedeBackoffice> listarSedes(String correlationId) {
        return paginar(propiedades.sitesPath(), Map.of(), PAGINA_SEDES, correlationId);
    }

    /** Los espacios de tipo AULA de una sede, en cualquier estado. */
    public List<UbicacionBackoffice> listarAulas(UUID siteId, String correlationId) {
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("siteId", siteId);
        filtros.put("type", "AULA");
        return paginar(propiedades.locationsPath(), filtros, PAGINA_UBICACIONES, correlationId);
    }

    private <T> List<T> paginar(String ruta, Map<String, Object> filtros,
                                ParameterizedTypeReference<Pagina<T>> tipo, String correlationId) {
        String base = urlBase();
        String token = tokens.token();
        List<T> resultado = new ArrayList<>();
        Set<String> cursoresVistos = new HashSet<>();
        String cursor = null;
        for (int pagina = 0; pagina < MAXIMO_PAGINAS; pagina++) {
            String cursorActual = cursor;
            // Valores como variables de plantilla: así se codifican completos (un cursor puede traer + o =).
            Function<UriBuilder, URI> uri = builder -> {
                Map<String, Object> variables = new HashMap<>();
                UriBuilder b = builder.path(ruta).queryParam("limit", "{limit}");
                variables.put("limit", propiedades.pageSize());
                filtros.forEach((nombre, valor) -> {
                    b.queryParam(nombre, "{" + nombre + "}");
                    variables.put(nombre, valor);
                });
                if (cursorActual != null) {
                    b.queryParam("cursor", "{cursor}");
                    variables.put("cursor", cursorActual);
                }
                return b.build(variables);
            };
            Pagina<T> respuesta = pedir(base, uri, tipo, token, correlationId, ruta);
            if (respuesta == null) break;
            if (respuesta.items() != null) resultado.addAll(respuesta.items());
            cursor = respuesta.nextCursor();
            if (cursor == null || cursor.isBlank()) return resultado;
            if (!cursoresVistos.add(cursor)) {
                throw new IntegracionExternaException("Backoffice repitió el cursor de paginación en " + ruta);
            }
        }
        if (cursor != null && !cursor.isBlank()) {
            throw new IntegracionExternaException("Backoffice devolvió más de " + MAXIMO_PAGINAS + " páginas en " + ruta);
        }
        return resultado;
    }

    private <T> Pagina<T> pedir(String base, Function<UriBuilder, URI> uri,
                                ParameterizedTypeReference<Pagina<T>> tipo, String token,
                                String correlationId, String ruta) {
        try {
            return http.get()
                    .uri(base, uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("X-Correlation-Id", correlationId)
                    .retrieve()
                    .body(tipo);
        } catch (RestClientResponseException ex) {
            throw new IntegracionExternaException("Backoffice respondió " + ex.getStatusCode().value()
                    + " en " + ruta + motivo(ex.getStatusCode().value()), ex);
        } catch (RestClientException ex) {
            throw new IntegracionExternaException("No se pudo leer " + ruta + " de Backoffice: " + ex.getMessage(), ex);
        }
    }

    private String urlBase() {
        String base = propiedades.baseUrl();
        if (base == null || base.isBlank()) {
            throw new IntegracionExternaException("Falta configurar BACKOFFICE_BASE_URL");
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private static String motivo(int estado) {
        return switch (estado) {
            case 401 -> ": el token de servicio no es válido para Backoffice";
            case 403 -> ": el token de servicio no tiene backoffice:read";
            default -> "";
        };
    }
}

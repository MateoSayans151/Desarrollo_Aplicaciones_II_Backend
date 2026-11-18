package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Entidades.EventoPublicado;
import ar.edu.universidad.gestionacademica.Repositorios.EventoPublicadoRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Arma el envelope de CORE para los eventos academic.* y lo deja en el outbox dentro de la
 * transacción actual. {@link EnviadorEventosCore} lo envía después del commit.
 * <p>
 * Eventos del catálogo: academic.exam-scheduled.v1 (examId, subjectId, startAt, endAt, siteId,
 * locationId) y academic.term-defined.v1 (termId, startDate, endDate). Se pueden sumar campos:
 * CORE los distribuye sin cambios.
 */
@Component
public class PublicadorEventosCore {
    public static final String PERIODO_DEFINIDO = "academic.term-defined.v1";
    public static final String EXAMEN_PROGRAMADO = "academic.exam-scheduled.v1";
    static final String SOURCE = "academic";
    static final String SERVICIO = "academic-service";

    private final PropiedadesEventosCore propiedades;
    private final EventoPublicadoRepositorio outbox;
    private final ObjectMapper json;
    private final Clock reloj;

    @Autowired
    public PublicadorEventosCore(PropiedadesEventosCore propiedades, EventoPublicadoRepositorio outbox,
                                 ObjectMapper json) {
        this(propiedades, outbox, json, Clock.systemUTC());
    }

    PublicadorEventosCore(PropiedadesEventosCore propiedades, EventoPublicadoRepositorio outbox,
                          ObjectMapper json, Clock reloj) {
        this.propiedades = propiedades;
        this.outbox = outbox;
        this.json = json;
        this.reloj = reloj;
    }

    /** false con CORE_EVENTS_PUBLISH_ENABLED=false: {@link #registrar} no hace nada. */
    public boolean habilitado() {
        return propiedades.publishEnabled();
    }

    /**
     * @param subject entidad a la que se refiere, por ejemplo {@code term/12}
     * @param data    payload; los valores tienen que ser serializables a JSON
     */
    public void registrar(String tipo, String subject, Map<String, Object> data) {
        if (!propiedades.publishEnabled()) return;
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("specVersion", "1.0");
        sobre.put("eventId", eventId);
        sobre.put("eventType", tipo);
        sobre.put("source", SOURCE);
        sobre.put("occurredAt", Instant.now(reloj).truncatedTo(ChronoUnit.MILLIS).toString());
        sobre.put("subject", subject);
        sobre.put("correlationId", correlationId());
        sobre.put("causationId", null);
        sobre.put("actor", actor(SecurityContextHolder.getContext().getAuthentication()));
        sobre.put("data", data);
        try {
            outbox.save(EventoPublicado.builder().eventId(eventId).tipo(tipo)
                    .sobre(json.writeValueAsString(sobre)).creadoEn(Instant.now(reloj)).build());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento " + tipo, ex);
        }
    }

    /** CORE exige un UUID: se reusa el X-Correlation-Id de la petición si lo es. */
    private static String correlationId() {
        String actual = MDC.get("correlationId");
        if (actual != null) {
            try {
                return UUID.fromString(actual).toString();
            } catch (IllegalArgumentException noEsUuid) {
                // Se genera uno nuevo.
            }
        }
        return UUID.randomUUID().toString();
    }

    /** Un usuario de CORE figura con su sub; la sesión local y los procesos, como el servicio. */
    static Map<String, String> actor(Authentication autenticacion) {
        if (autenticacion instanceof JwtAuthenticationToken jwt) {
            String servicio = jwt.getToken().getClaimAsString("service");
            if (servicio == null || servicio.isBlank()) {
                return Map.of("type", "user", "id", jwt.getToken().getSubject());
            }
        }
        return Map.of("type", "service", "id", SERVICIO);
    }
}

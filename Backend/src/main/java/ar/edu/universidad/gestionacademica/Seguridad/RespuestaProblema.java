package ar.edu.universidad.gestionacademica.Seguridad;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Escribe errores de los filtros de seguridad con la misma forma que ManejadorErrores. */
public final class RespuestaProblema {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(RespuestaProblema.class);

    private RespuestaProblema() { }

    public static void escribir(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
                                String codigo, String detalle) throws IOException {
        log.warn("Acceso rechazado {} {}: {} {} - {}", request.getMethod(), request.getRequestURI(),
                estado.value(), codigo, detalle);
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "https://uadenet.local/errors/" + codigo.toLowerCase().replace('_', '-'));
        problema.put("title", estado.getReasonPhrase());
        problema.put("status", estado.value());
        problema.put("detail", detalle);
        problema.put("instance", request.getRequestURI());
        problema.put("code", codigo);
        String correlationId = CorrelationId.de(request);
        problema.put("correlationId", correlationId);
        response.setStatus(estado.value());
        response.setHeader(CorrelationId.HEADER, correlationId);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JSON.writeValueAsString(problema));
    }
}

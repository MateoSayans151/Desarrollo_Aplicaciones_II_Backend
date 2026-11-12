package ar.edu.universidad.gestionacademica.Seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Propaga el X-Correlation-Id que manda CORE (o genera uno): lo devuelve en la respuesta,
 * lo deja en los logs (MDC) y lo usan los errores problem+json. Corre antes que Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationId extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final String ATRIBUTO = CorrelationId.class.getName();
    private static final int LARGO_MAXIMO = 100;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = de(request);
        response.setHeader(HEADER, id);
        MDC.put("correlationId", id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    /** El id de la petición actual; si todavía no tiene, lo genera y lo fija para toda la petición. */
    public static String de(HttpServletRequest request) {
        Object guardado = request.getAttribute(ATRIBUTO);
        if (guardado instanceof String id) return id;
        String recibido = request.getHeader(HEADER);
        String id = recibido == null || recibido.isBlank() || recibido.length() > LARGO_MAXIMO
                ? UUID.randomUUID().toString() : recibido.trim();
        request.setAttribute(ATRIBUTO, id);
        return id;
    }
}

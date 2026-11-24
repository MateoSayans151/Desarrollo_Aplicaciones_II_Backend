package ar.edu.universidad.gestionacademica.Observabilidad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Una línea de log por petición HTTP: método, ruta, status, duración y quién consume.
 * Corre justo después de {@code CorrelationId} y antes que Spring Security, así también
 * registra los 401/403. Deja {@code consumidor} en el MDC: aparece en todos los logs de la petición.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RegistroPeticiones extends OncePerRequestFilter {
    static final String MDC_CONSUMIDOR = "consumidor";
    static final String MDC_USUARIO = "usuario";
    private static final Logger log = LoggerFactory.getLogger("http");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // El healthcheck de Railway y Swagger sólo meten ruido.
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || uri.startsWith("/health") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs")
                || uri.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Consumidor consumidor = Consumidor.de(request);
        MDC.put(MDC_CONSUMIDOR, consumidor.nombre());
        if (consumidor.usuario() != null) MDC.put(MDC_USUARIO, consumidor.usuario());
        String ruta = request.getQueryString() == null
                ? request.getRequestURI() : request.getRequestURI() + "?" + request.getQueryString();
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
            registrar(request.getMethod(), ruta, response.getStatus(), inicio, consumidor);
        } catch (IOException | ServletException | RuntimeException ex) {
            log.error("HTTP {} {} -> excepción sin manejar {} consumidor={} ({})", request.getMethod(), ruta,
                    kv("duracionMs", ms(inicio)), consumidor.nombre(), ex.toString());
            throw ex;
        } finally {
            MDC.remove(MDC_CONSUMIDOR);
            MDC.remove(MDC_USUARIO);
        }
    }

    private void registrar(String metodo, String ruta, int status, long inicio, Consumidor consumidor) {
        // consumidor y usuario ya viajan en el MDC: en JSON salen como atributos sin repetirlos con kv.
        Object[] args = {metodo, ruta, kv("status", status), kv("duracionMs", ms(inicio)),
                consumidor.nombre(), oGuion(MDC.get(MDC_USUARIO)), kv("origin", oGuion(consumidor.origin()))};
        String mensaje = "HTTP {} {} -> {} {} consumidor={} usuario={} {}";
        if (status >= 500) log.error(mensaje, args);
        else if (status >= 400) log.warn(mensaje, args);
        else log.info(mensaje, args);
    }

    private static String oGuion(String valor) {
        return valor == null ? "-" : valor;
    }

    private static long ms(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}

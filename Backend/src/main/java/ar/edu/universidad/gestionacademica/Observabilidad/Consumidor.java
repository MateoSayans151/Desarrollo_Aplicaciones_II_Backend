package ar.edu.universidad.gestionacademica.Observabilidad;

import ar.edu.universidad.gestionacademica.Seguridad.FiltroFirmaCore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Quién nos está llamando, deducido de los headers de la petición. Sólo sirve para los logs:
 * lee los claims del JWT <b>sin verificar la firma</b> (eso lo hace Spring Security después).
 * Si el token es falso, la misma línea de log muestra el 401.
 *
 * <ul>
 *   <li>{@code servicio:backoffice}: token de servicio (claim {@code service}), llamada directa a /internal/v1.</li>
 *   <li>{@code core-gateway[STUDENT]} / {@code core-gateway[TEACHER]} / ...: JWT de persona que pasó por
 *       el gateway de CORE (trae {@code X-Core-Signature}, {@code X-Core-Timestamp} o {@code X-User-Id}).
 *       STUDENT suele ser el Portal de Alumnos y TEACHER el Portal de Docencia.</li>
 *   <li>{@code jwt-directo[...]}: JWT de CORE sin headers del gateway.</li>
 *   <li>{@code frontend-propio}: sesión local (cookie JSESSIONID) o login local.</li>
 *   <li>{@code anonimo}: sin credenciales.</li>
 * </ul>
 */
public record Consumidor(String nombre, String usuario, String origin) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static Consumidor de(HttpServletRequest request) {
        String origin = primero(request.getHeader(HttpHeaders.ORIGIN), request.getHeader(HttpHeaders.REFERER));
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean gateway = presente(request.getHeader(FiltroFirmaCore.FIRMA))
                || presente(request.getHeader(FiltroFirmaCore.TIMESTAMP))
                || presente(request.getHeader(FiltroFirmaCore.USUARIO));

        if (autorizacion != null && autorizacion.regionMatches(true, 0, "Bearer ", 0, 7)) {
            JsonNode claims = claims(autorizacion.substring(7).trim());
            String servicio = texto(claims, "service");
            String sub = primero(texto(claims, "sub"), request.getHeader(FiltroFirmaCore.USUARIO));
            if (servicio != null) return new Consumidor("servicio:" + servicio, sub, origin);
            List<String> roles = roles(claims, request.getHeader("X-User-Roles"));
            String sufijo = roles.isEmpty() ? "" : roles.toString();
            return new Consumidor((gateway ? "core-gateway" : "jwt-directo") + sufijo, sub, origin);
        }
        if (tieneSesion(request) || "/api/auth/login".equals(request.getRequestURI())) {
            return new Consumidor("frontend-propio", null, origin);
        }
        return new Consumidor("anonimo", null, origin);
    }

    private static JsonNode claims(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length < 2) return null;
            return JSON.readTree(new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return null;
        }
    }

    private static List<String> roles(JsonNode claims, String headerRoles) {
        List<String> roles = new ArrayList<>();
        JsonNode nodo = claims == null ? null : claims.get("roles");
        if (nodo != null && nodo.isArray()) nodo.forEach(r -> roles.add(r.asText()));
        else if (nodo != null && nodo.isTextual()) agregar(roles, nodo.asText());
        if (roles.isEmpty() && headerRoles != null) agregar(roles, headerRoles);
        return roles;
    }

    private static void agregar(List<String> roles, String texto) {
        for (String parte : texto.split("[\\s,]+")) if (!parte.isBlank()) roles.add(parte.trim());
    }

    private static boolean tieneSesion(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) if ("JSESSIONID".equals(cookie.getName())) return true;
        return false;
    }

    private static String texto(JsonNode nodo, String campo) {
        if (nodo == null || !nodo.hasNonNull(campo)) return null;
        String valor = nodo.get(campo).asText();
        return valor.isBlank() ? null : valor;
    }

    private static boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String primero(String a, String b) {
        return presente(a) ? a : (presente(b) ? b : null);
    }
}

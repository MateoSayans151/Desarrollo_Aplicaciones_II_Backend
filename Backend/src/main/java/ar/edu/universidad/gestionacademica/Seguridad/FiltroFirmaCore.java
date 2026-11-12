package ar.edu.universidad.gestionacademica.Seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Verifica que una petición con JWT a /api/v1/** haya pasado por el gateway de CORE.
 * CORE firma con HMAC-SHA256 (hex) estas líneas unidas por salto de línea, sin salto final:
 * método, ruta de destino sin query, X-User-Id, X-Correlation-Id y X-Core-Timestamp.
 * Sin secreto configurado el filtro no actúa: así se puede probar antes de recibirlo.
 */
public class FiltroFirmaCore extends OncePerRequestFilter {
    public static final String FIRMA = "X-Core-Signature";
    public static final String TIMESTAMP = "X-Core-Timestamp";
    public static final String USUARIO = "X-User-Id";

    private final byte[] secreto;
    private final Duration tolerancia;
    private final Clock reloj;

    public FiltroFirmaCore(String secreto, Duration tolerancia) {
        this(secreto, tolerancia, Clock.systemUTC());
    }

    FiltroFirmaCore(String secreto, Duration tolerancia, Clock reloj) {
        this.secreto = secreto == null || secreto.isBlank() ? null : secreto.getBytes(StandardCharsets.UTF_8);
        this.tolerancia = tolerancia;
        this.reloj = reloj;
    }

    public boolean activo() {
        return secreto != null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
        return !activo()
                || !request.getRequestURI().startsWith("/api/v1/")
                || autorizacion == null
                || !autorizacion.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String firma = request.getHeader(FIRMA);
        String timestamp = request.getHeader(TIMESTAMP);
        if (firma == null || firma.isBlank() || timestamp == null || timestamp.isBlank()) {
            rechazar(request, response, "La petición no trae la firma del gateway de CORE");
            return;
        }
        Instant emitida = instante(timestamp.trim());
        if (emitida == null || Duration.between(emitida, reloj.instant()).abs().compareTo(tolerancia) > 0) {
            rechazar(request, response, "La firma del gateway de CORE venció o tiene un timestamp inválido");
            return;
        }
        String esperada = firmar(String.join("\n",
                request.getMethod().toUpperCase(Locale.ROOT),
                request.getRequestURI(),
                valor(request.getHeader(USUARIO)),
                valor(request.getHeader(CorrelationId.HEADER)),
                timestamp.trim()));
        if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                firma.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            rechazar(request, response, "La firma del gateway de CORE no es válida");
            return;
        }
        chain.doFilter(request, response);
    }

    String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular HMAC-SHA256", ex);
        }
    }

    /** Epoch en segundos o milisegundos, o ISO-8601. */
    static Instant instante(String valor) {
        if (valor.chars().allMatch(Character::isDigit)) {
            try {
                long numero = Long.parseLong(valor);
                return valor.length() >= 13 ? Instant.ofEpochMilli(numero) : Instant.ofEpochSecond(numero);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static String valor(String header) {
        return header == null ? "" : header;
    }

    private static void rechazar(HttpServletRequest request, HttpServletResponse response, String detalle)
            throws IOException {
        RespuestaProblema.escribir(request, response, HttpStatus.UNAUTHORIZED, "INVALID_GATEWAY_SIGNATURE", detalle);
    }
}

package ar.edu.universidad.gestionacademica.Observabilidad;

import ar.edu.universidad.gestionacademica.Excepciones.CredencialesInvalidasException;
import ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException;
import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolationException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Registra cada llamada a un controlador: qué función se ejecutó, con qué parámetros, qué devolvió,
 * cuánto tardó y, si falló, con qué error. Las claves sensibles (password, token, secret...) se enmascaran
 * y los JSON largos se recortan a {@code logging.llamadas.largo-maximo} caracteres.
 * Los errores se registran sin stack trace: los inesperados ya lo imprime {@code ManejadorErrores}.
 */
@Aspect
@Component
public class RegistroLlamadas {
    private static final Logger log = LoggerFactory.getLogger("llamadas");
    private static final Pattern SENSIBLE = Pattern.compile("(?i).*(password|contrase|clave|secret|token|authorization).*");

    private final ObjectMapper json;
    private final boolean habilitado;
    private final int largoMaximo;

    public RegistroLlamadas(ObjectMapper json,
                            @Value("${logging.llamadas.habilitado:true}") boolean habilitado,
                            @Value("${logging.llamadas.largo-maximo:2000}") int largoMaximo) {
        this.json = json;
        this.habilitado = habilitado;
        this.largoMaximo = largoMaximo;
    }

    @Around("within(ar.edu.universidad.gestionacademica.Controllers..*)"
            + " && @within(org.springframework.web.bind.annotation.RestController)"
            + " && !within(ar.edu.universidad.gestionacademica.Controllers.HealthController)")
    public Object registrar(ProceedingJoinPoint llamada) throws Throwable {
        if (!habilitado) return llamada.proceed();
        MethodSignature firma = (MethodSignature) llamada.getSignature();
        String funcion = firma.getDeclaringType().getSimpleName() + "." + firma.getName();
        String usuario = usuarioActual();
        if (usuario != null) MDC.put(RegistroPeticiones.MDC_USUARIO, usuario);

        log.info("ENTRADA {} usuario={} params={}", kv("funcion", funcion), usuario == null ? "-" : usuario,
                aJson(parametros(firma, llamada.getArgs())));
        long inicio = System.nanoTime();
        try {
            Object resultado = llamada.proceed();
            log.info("SALIDA {} {} resultado={}", kv("funcion", funcion), kv("duracionMs", ms(inicio)),
                    salida(resultado, firma.getReturnType()));
            return resultado;
        } catch (Throwable error) {
            Object[] args = {kv("funcion", funcion), kv("duracionMs", ms(inicio)),
                    kv("error", error.getClass().getSimpleName()), error.getMessage()};
            if (esperado(error)) log.warn("ERROR {} {} {} mensaje=\"{}\"", args);
            else log.error("ERROR {} {} {} mensaje=\"{}\"", args);
            throw error;
        }
    }

    /** Errores de negocio, validación o permisos: son respuestas 4xx previstas, no fallas del servicio. */
    private static boolean esperado(Throwable error) {
        return error instanceof ReglaNegocioException || error instanceof RecursoNoEncontradoException
                || error instanceof CredencialesInvalidasException || error instanceof IntegracionExternaException
                || error instanceof AccessDeniedException || error instanceof AuthenticationException
                || error instanceof ConstraintViolationException || error instanceof DataIntegrityViolationException;
    }

    private static String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) return null;
        if (auth instanceof JwtAuthenticationToken jwt) {
            String servicio = jwt.getToken().getClaimAsString("service");
            return servicio != null ? "service:" + servicio : jwt.getName();
        }
        return auth.getName();
    }

    private static Map<String, Object> parametros(MethodSignature firma, Object[] valores) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        String[] nombres = firma.getParameterNames();
        for (int i = 0; i < valores.length; i++) {
            Object valor = valores[i];
            if (valor instanceof ServletRequest || valor instanceof ServletResponse || valor instanceof HttpSession
                    || valor instanceof Principal || valor instanceof Authentication || valor instanceof BindingResult) {
                continue;
            }
            mapa.put(nombres != null && i < nombres.length ? nombres[i] : "arg" + i, valor);
        }
        return mapa;
    }

    private String salida(Object resultado, Class<?> tipo) {
        if (tipo == void.class) return "(sin cuerpo)";
        if (resultado instanceof ResponseEntity<?> respuesta) {
            Map<String, Object> mapa = new LinkedHashMap<>();
            mapa.put("status", respuesta.getStatusCode().value());
            mapa.put("body", respuesta.getBody());
            return cantidad(respuesta.getBody()) + aJson(mapa);
        }
        return cantidad(resultado) + aJson(resultado);
    }

    private static String cantidad(Object valor) {
        return valor instanceof Collection<?> lista ? "(" + lista.size() + " elementos) " : "";
    }

    String aJson(Object valor) {
        String texto;
        try {
            JsonNode arbol = json.valueToTree(valor);
            enmascarar(arbol);
            texto = json.writeValueAsString(arbol);
        } catch (Exception ex) {
            texto = String.valueOf(valor);
        }
        return texto.length() <= largoMaximo ? texto
                : texto.substring(0, largoMaximo) + "...(recortado, " + texto.length() + " caracteres)";
    }

    private static void enmascarar(JsonNode nodo) {
        if (nodo == null) return;
        if (nodo instanceof ObjectNode objeto) {
            Iterator<Map.Entry<String, JsonNode>> campos = objeto.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                if (SENSIBLE.matcher(campo.getKey()).matches()) campo.setValue(objeto.textNode("***"));
                else enmascarar(campo.getValue());
            }
        } else if (nodo.isArray()) {
            nodo.forEach(RegistroLlamadas::enmascarar);
        }
    }

    private static long ms(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000;
    }
}

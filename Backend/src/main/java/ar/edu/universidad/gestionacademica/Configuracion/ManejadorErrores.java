package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Excepciones.CredencialesInvalidasException;
import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Errores HTTP con el formato application/problem+json acordado con CORE.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                      HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado", "RESOURCE_NOT_FOUND",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Object> manejarReglaNegocio(ReglaNegocioException ex,
                                                      HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Regla de negocio", "BUSINESS_RULE_VIOLATION",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Object> manejarCredenciales(CredencialesInvalidasException ex,
                                                      HttpServletRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "INVALID_CREDENTIALS",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> manejarAutenticacion(AuthenticationException ex,
                                                       HttpServletRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, "No autenticado", "UNAUTHORIZED",
                "Iniciá sesión para continuar", request, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> manejarRestricciones(ConstraintViolationException ex,
                                                       HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "VALIDATION_ERROR",
                "Hay datos inválidos en la solicitud", request, null);
    }

    /** Red de seguridad ante carreras concurrentes: la base rechaza lo que ya pasó las validaciones. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> manejarIntegridad(DataIntegrityViolationException ex,
                                                    HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "Conflicto de datos", "DATA_CONFLICT",
                "El registro ya existe o entra en conflicto con otros datos. Actualizá la lista y revisá los valores",
                request, null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> manejarAccesoDatos(DataAccessException ex,
                                                     HttpServletRequest request) {
        log.error("Error de persistencia en {}", request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error de persistencia", "DATA_ACCESS_ERROR",
                "No se pudieron leer los datos por una inconsistencia de la base", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "INTERNAL_ERROR",
                "Ocurrió un error inesperado al procesar la solicitud", request, null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "VALIDATION_ERROR",
                "Hay datos inválidos en la solicitud", servlet(request), errores);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "VALIDATION_ERROR",
                "El cuerpo de la solicitud no es válido: revisá el JSON, las fechas, las horas y los valores permitidos",
                servlet(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "VALIDATION_ERROR",
                "Hay un parámetro con formato inválido en la solicitud", servlet(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus estado = HttpStatus.valueOf(statusCode.value());
        String codigo = estado.is4xxClientError() ? "BAD_REQUEST" : "INTERNAL_ERROR";
        String detalle = switch (estado) {
            case NOT_FOUND -> "La ruta solicitada no existe";
            case METHOD_NOT_ALLOWED -> "El método HTTP no está permitido para esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "El tipo de contenido no está soportado; usá application/json";
            case BAD_REQUEST -> "La solicitud está incompleta o es inválida";
            default -> "No se pudo procesar la solicitud";
        };
        return respuesta(estado, estado.getReasonPhrase(), codigo, detalle, servlet(request), null);
    }

    private HttpServletRequest servlet(WebRequest request) {
        return ((ServletWebRequest) request).getRequest();
    }

    private ResponseEntity<Object> respuesta(HttpStatus estado, String titulo, String codigo,
                                             String detalle, HttpServletRequest request,
                                             Map<String, String> errores) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://uadenet.local/errors/" + codigo.toLowerCase().replace('_', '-')));
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("code", codigo);
        problema.setProperty("correlationId", correlationId(request));
        if (errores != null && !errores.isEmpty()) problema.setProperty("errors", errores);
        return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problema);
    }

    private String correlationId(HttpServletRequest request) {
        String recibido = request.getHeader("X-Correlation-Id");
        return recibido == null || recibido.isBlank() ? UUID.randomUUID().toString() : recibido;
    }
}

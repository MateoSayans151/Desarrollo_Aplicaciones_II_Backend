package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Errores HTTP con el formato application/problem+json acordado con CORE.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarNoEncontrado(RecursoNoEncontradoException ex,
                                                              HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, "Recurso no encontrado", "RESOURCE_NOT_FOUND",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ProblemDetail> manejarReglaNegocio(ReglaNegocioException ex,
                                                              HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST, "Regla de negocio", "BUSINESS_RULE_VIOLATION",
                ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarValidacion(MethodArgumentNotValidException ex,
                                                            HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", "VALIDATION_ERROR",
                "Hay datos inválidos en la solicitud", request, errores);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> manejarAccesoDatos(DataAccessException ex,
                                                             HttpServletRequest request) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error de persistencia", "DATA_ACCESS_ERROR",
                "No se pudieron leer los datos por una inconsistencia de la base", request, null);
    }

    private ResponseEntity<ProblemDetail> respuesta(HttpStatus estado, String titulo, String codigo,
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

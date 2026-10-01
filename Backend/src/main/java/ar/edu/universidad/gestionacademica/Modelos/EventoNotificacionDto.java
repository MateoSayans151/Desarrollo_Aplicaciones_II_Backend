package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.EstadoInscripcionCurso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Contrato de integración para notificar la publicación de un resultado.
 * La aplicación todavía no publica este evento en un broker ni por SSE/WebSocket.
 */
public record EventoNotificacionDto(
        String tipo,
        OffsetDateTime ocurridoEn,
        String alumnoId,
        Long cursoId,
        BigDecimal notaFinal,
        EstadoInscripcionCurso estado,
        LocalDate fechaResultado,
        String enlace) {
}

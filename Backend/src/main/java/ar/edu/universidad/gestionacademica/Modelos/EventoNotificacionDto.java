package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.EstadoInscripcionCurso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Contrato de integración para notificar la publicación de un resultado.
 * La aplicación todavía no publica este evento en un broker ni por SSE/WebSocket.
 */
public record EventoNotificacionDto(
        String eventId,
        String sourceModule,
        String eventType,
        Instant occurredAt,
        Payload payload) {

    public record Payload(
            String alumnoId,
            Long cursoId,
            String materia,
            String comision,
            String docente,
            String sede,
            String cuatrimestre,
            BigDecimal notaFinal,
            EstadoInscripcionCurso estado,
            boolean aprobado,
            LocalDate fechaResultado) { }
}

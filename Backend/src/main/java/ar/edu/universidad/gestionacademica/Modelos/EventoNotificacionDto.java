package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.EstadoInscripcionCurso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Contrato legado del publicador Rabbit opcional de Analítica.
 * Todavía debe reemplazarse por el envelope y los eventType definidos por CORE.
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

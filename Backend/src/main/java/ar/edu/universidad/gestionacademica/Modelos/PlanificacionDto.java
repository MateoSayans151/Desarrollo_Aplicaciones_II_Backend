package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public final class PlanificacionDto {
    private PlanificacionDto() { }

    public record CrearSedeDto(@NotBlank String nombre, @NotBlank String direccion) { }
    public record SedeRespuestaDto(Long id, String nombre, String direccion) { }

    public record CrearAulaDto(@NotBlank String codigo, @Positive int capacidadMaxima) { }
    public record AulaRespuestaDto(Long id, String codigo, int capacidadMaxima, Long sedeId) { }

    public record CrearAsignacionDto(@NotNull Long aulaId, @NotNull Long asignaturaId,
                                     @NotNull LocalDate fecha, @NotNull LocalTime horaInicio,
                                     @NotNull LocalTime horaFin, @Positive int cantidadEstudiantes) { }
    public record AsignacionRespuestaDto(Long id, Long aulaId, Long asignaturaId, LocalDate fecha,
                                         LocalTime horaInicio, LocalTime horaFin, int cantidadEstudiantes) { }

    public record CrearPeriodoDto(@Min(2000) int anio, @Min(1) @Max(2) int numero,
                                  @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) { }
    public record PeriodoRespuestaDto(Long id, int anio, int numero, LocalDate fechaInicio,
                                      LocalDate fechaFin) { }

    public record CrearTurnoDto(@NotBlank String nombre, @NotNull LocalDate fechaInicio,
                                @NotNull LocalDate fechaFin, @NotNull LocalDate inscripcionDesde,
                                @NotNull LocalDate inscripcionHasta) { }
    public record TurnoRespuestaDto(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                    LocalDate inscripcionDesde, LocalDate inscripcionHasta) { }
}

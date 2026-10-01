package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.*;
import ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class PlanificacionDto {
    private PlanificacionDto() { }

    public record CrearSedeDto(@NotBlank String nombre, @NotBlank String direccion) { }
    public record SedeRespuestaDto(Long id, String nombre, String direccion, UUID backofficeSiteId,
                                   String estado) { }

    public record CrearAulaDto(@NotBlank String codigo, @Positive int capacidadMaxima) { }
    public record AulaRespuestaDto(Long id, String codigo, String nombre, UUID backofficeLocationId, TipoUbicacion tipo,
                                   int capacidadMaxima, Long sedeId, String estado) { }

    public record CrearAsignacionDto(@NotNull Long aulaId, @NotNull Long asignaturaId,
                                     @NotNull LocalDate fecha, @NotNull LocalTime horaInicio,
                                     @NotNull LocalTime horaFin, @Positive int cantidadEstudiantes) { }
    public record AsignacionRespuestaDto(Long id, Long aulaId, Long asignaturaId, LocalDate fecha,
                                         LocalTime horaInicio, LocalTime horaFin, int cantidadEstudiantes) { }

    public record CrearPeriodoDto(@Min(2000) int anio, @Min(1) @Max(2) int numero,
                                  @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin,
                                  @NotNull LocalDate inscripcionDesde, @NotNull LocalDate inscripcionHasta) { }
    public record PeriodoRespuestaDto(Long id, int anio, int numero, LocalDate fechaInicio,
                                      LocalDate fechaFin, LocalDate inscripcionDesde,
                                      LocalDate inscripcionHasta) { }

    public record CrearTurnoDto(@NotBlank String nombre, @NotNull LocalDate fechaInicio,
                                @NotNull LocalDate fechaFin, @NotNull LocalDate inscripcionDesde,
                                @NotNull LocalDate inscripcionHasta) { }
    public record TurnoRespuestaDto(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                    LocalDate inscripcionDesde, LocalDate inscripcionHasta) { }
}

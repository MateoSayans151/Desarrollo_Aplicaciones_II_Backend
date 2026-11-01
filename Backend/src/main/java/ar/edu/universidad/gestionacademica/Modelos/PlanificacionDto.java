package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.*;
import ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class PlanificacionDto {
    private PlanificacionDto() { }

    public record CrearSedeDto(@NotBlank @Size(max = 255) String nombre,
                               @NotBlank @Size(max = 255) String direccion) { }
    public record SedeRespuestaDto(Long id, String nombre, String direccion, UUID backofficeSiteId,
                                   String estado) { }

    public record CrearAulaDto(@NotBlank @Size(max = 30) String codigo, @Min(1) @Max(100) int capacidadMaxima,
                               @Size(max = 255) String nombre) { }
    public record AulaRespuestaDto(Long id, String codigo, String nombre, UUID backofficeLocationId, TipoUbicacion tipo,
                                   int capacidadMaxima, Long sedeId, String estado) { }

    public record CrearAsignacionDto(@NotNull Long aulaId, @NotNull Long asignaturaId,
                                     @NotNull LocalDate fecha, @NotNull LocalTime horaInicio,
                                     @NotNull LocalTime horaFin, @Positive int cantidadEstudiantes) { }
    public record AsignacionRespuestaDto(Long id, Long aulaId, Long asignaturaId, LocalDate fecha,
                                         LocalTime horaInicio, LocalTime horaFin, int cantidadEstudiantes) { }

    public enum TipoOcupacionAula { CURSO, ASIGNACION }
    public record OcupacionAulaDto(TipoOcupacionAula tipo, Long referenciaId, Long aulaId,
                                   Long asignaturaId, String asignaturaCodigo,
                                   String asignaturaNombre, String cursoCodigo, LocalDate fecha,
                                   LocalTime horaInicio, LocalTime horaFin,
                                   Integer cantidadEstudiantes) { }

    public record CrearPeriodoDto(@Min(2000) @Max(2100) int anio, @Min(1) @Max(2) int numero,
                                  @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin,
                                  @NotNull LocalDate inscripcionDesde, @NotNull LocalDate inscripcionHasta) { }
    public record PeriodoRespuestaDto(Long id, int anio, int numero, LocalDate fechaInicio,
                                      LocalDate fechaFin, LocalDate inscripcionDesde,
                                      LocalDate inscripcionHasta) { }

    public record CrearTurnoDto(@NotBlank @Size(max = 255) String nombre, @NotNull LocalDate fechaInicio,
                                @NotNull LocalDate fechaFin, @NotNull LocalDate inscripcionDesde,
                                @NotNull LocalDate inscripcionHasta) { }
    public record TurnoRespuestaDto(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                    LocalDate inscripcionDesde, LocalDate inscripcionHasta) { }
}

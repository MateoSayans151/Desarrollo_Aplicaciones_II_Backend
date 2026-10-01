package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class CursoDto {
    private CursoDto() { }

    public record CrearCursoDto(
            @NotBlank String codigo,
            @NotNull Long asignaturaId,
            @NotNull Long periodoId,
            @NotNull Long sedeId,
            @NotNull ModalidadCurso modalidad,
            @Positive int cupoMaximo,
            @NotNull LocalDate fechaInicio,
            @NotNull LocalDate fechaFin) { }

    public record CursoRespuestaDto(Long id, String codigo, Long asignaturaId, Long periodoId, Long sedeId,
                                    ModalidadCurso modalidad, EstadoCurso estado, int cupoMaximo,
                                    LocalDate fechaInicio, LocalDate fechaFin, long inscriptosActivos) { }

    public record ActualizarEstadoCursoDto(@NotNull EstadoCurso estado) { }

    public record CrearCursoDocenteDto(@NotBlank String docenteId, @NotNull RolDocenteCurso rol) { }
    public record CursoDocenteRespuestaDto(Long id, String docenteId, RolDocenteCurso rol) { }

    public record CrearInscripcionCursoDto(@NotBlank String alumnoId) { }
    public record ActualizarInscripcionCursoDto(@NotNull EstadoInscripcionCurso estado,
                                                 @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal notaFinal,
                                                 LocalDate fechaResultado) { }
    public record InscripcionCursoRespuestaDto(Long id, String alumnoId, LocalDate fechaInscripcion,
                                               EstadoInscripcionCurso estado, BigDecimal notaFinal,
                                               LocalDate fechaResultado) { }

    public record CrearHorarioCursoDto(@NotNull Long aulaId, @NotNull DayOfWeek diaSemana,
                                       @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) { }
    public record HorarioCursoRespuestaDto(Long id, Long aulaId, DayOfWeek diaSemana,
                                           LocalTime horaInicio, LocalTime horaFin) { }

    public record AsignaturaResumenDto(Long id, String codigo, String nombre) { }
    public record PeriodoResumenDto(Long id, int anio, int numero, LocalDate fechaInicio,
                                    LocalDate fechaFin, LocalDate inscripcionDesde,
                                    LocalDate inscripcionHasta) { }
    public record CursoAcademicoRespuestaDto(Long id, String codigo, AsignaturaResumenDto asignatura,
                                             PeriodoResumenDto periodo, Long sedeId, ModalidadCurso modalidad,
                                             EstadoCurso estado, int cupoMaximo, LocalDate fechaInicio,
                                             LocalDate fechaFin, long inscriptosActivos) { }
    public record CursoDetalleAcademicoRespuestaDto(CursoAcademicoRespuestaDto curso,
                                                    List<CursoDocenteRespuestaDto> docentes,
                                                    List<InscripcionCursoRespuestaDto> inscripciones,
                                                    List<HorarioCursoRespuestaDto> horarios) { }
}

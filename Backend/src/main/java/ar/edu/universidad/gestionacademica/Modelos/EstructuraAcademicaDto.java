package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import ar.edu.universidad.gestionacademica.Entidades.EstadoAcademico;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class EstructuraAcademicaDto {
    private EstructuraAcademicaDto() { }

    public record CrearCarreraDto(@NotBlank String codigo, @NotBlank String nombre,
                                  @NotBlank String facultad, @NotBlank String duracion,
                                  @NotBlank String titulo) { }
    public record CarreraRespuestaDto(Long id, String codigo, String nombre, String facultad,
                                      String duracion, String titulo, EstadoAcademico estado) { }

    public record CrearPlanDto(@NotBlank String codigo, @NotBlank String nombre,
                               @NotNull LocalDate vigenciaDesde,
                               @PositiveOrZero int cantidadAsignaturas) { }
    public record PlanRespuestaDto(Long id, String codigo, String nombre, LocalDate vigenciaDesde,
                                   int cantidadAsignaturas, EstadoAcademico estado, Long carreraId) { }

    public record CrearAsignaturaDto(@NotBlank String codigo, @NotBlank String nombre,
                                     @Min(1) int anio, @Positive int creditos,
                                     @Positive int cargaHoraria) { }
    public record AsignaturaRespuestaDto(Long id, String codigo, String nombre, int anio,
                                         int creditos, int cargaHoraria, EstadoAcademico estado,
                                         Long planId) { }

    public record CrearCorrelatividadDto(@NotNull Long correlativaId,
                                         @NotNull Correlatividad.Tipo tipo) { }
    public record CorrelatividadRespuestaDto(Long id, Long asignaturaId, Long correlativaId,
                                             Correlatividad.Tipo tipo) { }
}

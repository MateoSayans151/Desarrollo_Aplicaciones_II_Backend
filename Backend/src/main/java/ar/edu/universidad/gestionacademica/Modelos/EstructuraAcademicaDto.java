package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class EstructuraAcademicaDto {
    private EstructuraAcademicaDto() { }

    public record CrearCarreraDto(@NotBlank String codigo, @NotBlank String nombre) { }
    public record CarreraRespuestaDto(Long id, String codigo, String nombre, boolean activa) { }

    public record CrearPlanDto(@NotBlank String version, @NotNull LocalDate vigenciaDesde,
                               LocalDate vigenciaHasta) { }
    public record PlanRespuestaDto(Long id, String version, LocalDate vigenciaDesde,
                                   LocalDate vigenciaHasta, Long carreraId) { }

    public record CrearAsignaturaDto(@NotBlank String codigo, @NotBlank String nombre,
                                     @Min(1) @Max(10) int cuatrimestreSugerido,
                                     @Positive int cargaHoraria) { }
    public record AsignaturaRespuestaDto(Long id, String codigo, String nombre,
                                         int cuatrimestreSugerido, int cargaHoraria, Long planId) { }

    public record CrearCorrelatividadDto(@NotNull Long correlativaId,
                                         @NotNull Correlatividad.Tipo tipo) { }
    public record CorrelatividadRespuestaDto(Long id, Long asignaturaId, Long correlativaId,
                                             Correlatividad.Tipo tipo) { }
}

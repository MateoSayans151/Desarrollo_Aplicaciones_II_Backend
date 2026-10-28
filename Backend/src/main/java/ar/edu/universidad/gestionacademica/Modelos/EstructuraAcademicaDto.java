package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import ar.edu.universidad.gestionacademica.Entidades.EstadoAcademico;
import ar.edu.universidad.gestionacademica.Entidades.EstadoPlanEstudio;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public final class EstructuraAcademicaDto {
    private EstructuraAcademicaDto() { }

    public record CrearCarreraDto(
                                  @NotBlank
                                  @Pattern(regexp = "^[A-Z]{3}[0-9]?-[0-9]{3}$",
                                           message = "debe tener el formato ARQ-001 o ARQ1-001")
                                  String codigo, @NotBlank String nombre,
                                  @NotBlank String facultad, @NotBlank String titulo) { }
    public record CarreraRespuestaDto(Long id, String codigo, String nombre, String facultad,
                                      String duracion, String titulo, EstadoAcademico estado,
                                      PlanRespuestaDto ultimoPlanActivo) { }

    public record CrearPlanDto(
                               @NotBlank
                               @Pattern(regexp = "^[A-Z]{3}[0-9]?-[0-9]{3}$",
                                        message = "debe tener el formato ARQ-001 o ARQ1-001")
                               String codigo, @NotBlank String nombre,
                               @NotNull LocalDate vigenciaDesde) { }
    public record ActualizarPlanDto(
                                    @NotBlank
                                    @Pattern(regexp = "^[A-Z]{3}[0-9]?-[0-9]{3}$",
                                             message = "debe tener el formato ARQ-001 o ARQ1-001")
                                    String codigo, @NotBlank String nombre,
                                    @NotNull LocalDate vigenciaDesde,
                                    @NotNull EstadoPlanEstudio estado) { }
    public record PlanRespuestaDto(Long id, String codigo, String nombre, LocalDate vigenciaDesde,
                                   int cantidadAsignaturas, EstadoPlanEstudio estado, Long carreraId) { }

    public record CrearAsignaturaDto(
                                     @NotBlank
                                     @Pattern(regexp = "^[A-Z]{3}[0-9]?-[0-9]{3}$",
                                              message = "debe tener el formato ARQ-001 o ARQ1-001")
                                     String codigo, @NotBlank String nombre,
                                     @Min(1) int anio, @Positive int creditos,
                                     @Positive int cargaHoraria) { }
    public record AsignaturaRespuestaDto(Long id, String codigo, String nombre, int anio,
                                         int creditos, int cargaHoraria, EstadoAcademico estado,
                                         Long planId) { }

    public record CrearCorrelatividadDto(@NotNull Long correlativaId,
                                         @NotNull Correlatividad.Tipo tipo) { }
    public record CorrelatividadRespuestaDto(Long id, Long asignaturaId, Long correlativaId,
                                             Correlatividad.Tipo tipo) { }
    public record MateriaCorrelativaDto(Long id, String codigo, String nombre) { }
    public record CondicionCorrelatividadDto(Correlatividad.Tipo tipo,
                                             List<MateriaCorrelativaDto> materiasCorrelativas) { }
    public record CondicionesRegularidadRespuestaDto(Long asignaturaId,
                                                      List<CondicionCorrelatividadDto> condiciones) { }
}

package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class RegularidadDto {
    private RegularidadDto() { }

    public record GuardarCondicionesRegularidadDto(
            @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal asistenciaMinima,
            @NotNull @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal promedioMinimo) { }

    public record CondicionesRegularidadCursoDto(Long id, Long cursoId, String codigo,
                                                  BigDecimal asistenciaMinima,
                                                  BigDecimal promedioMinimo) { }
}

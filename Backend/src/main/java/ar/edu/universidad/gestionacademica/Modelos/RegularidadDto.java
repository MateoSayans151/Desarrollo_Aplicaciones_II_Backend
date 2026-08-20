package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.util.List;

public final class RegularidadDto {
    private RegularidadDto() { }

    public record ValidarRegularidadDto(@DecimalMin("0") @DecimalMax("100") double asistencia,
                                        @DecimalMin("0") @DecimalMax("10") double promedio,
                                        @DecimalMin("0") @DecimalMax("100") double asistenciaMinima,
                                        @DecimalMin("0") @DecimalMax("10") double promedioMinimo) { }
    public record ResultadoRegularidadDto(boolean regular, boolean habilitadoParaFinal,
                                          List<String> motivos) { }
}

package ar.edu.universidad.gestionacademica.BusinessLogic;

import org.junit.jupiter.api.Test;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.ValidarRegularidadDto;
import static org.assertj.core.api.Assertions.assertThat;

class RegularidadServicioTest {
    private final RegularidadServicio servicio = new RegularidadServicio();

    @Test
    void habilitaCuandoCumpleAmbasCondiciones() {
        var resultado = servicio.validar(new ValidarRegularidadDto(80, 7, 75, 6));
        assertThat(resultado.regular()).isTrue();
        assertThat(resultado.habilitadoParaFinal()).isTrue();
    }

    @Test
    void informaTodasLasCondicionesIncumplidas() {
        var resultado = servicio.validar(new ValidarRegularidadDto(60, 4, 75, 6));
        assertThat(resultado.regular()).isFalse();
        assertThat(resultado.motivos()).hasSize(2);
    }
}

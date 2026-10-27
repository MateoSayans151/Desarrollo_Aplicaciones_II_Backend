package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.RegularidadCurso;
import ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.GuardarCondicionesRegularidadDto;
import ar.edu.universidad.gestionacademica.Repositorios.CursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.RegularidadCursoRepositorio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RegularidadServicioTest {
    private final CursoRepositorio cursos = mock(CursoRepositorio.class);
    private final RegularidadCursoRepositorio regularidades = mock(RegularidadCursoRepositorio.class);
    private final RegularidadServicio servicio = new RegularidadServicio(cursos, regularidades);
    private final Curso curso = Curso.builder().id(3L).codigo("INF-201-A").build();

    @Test
    void consultaLasCondicionesSinSolicitarUnAlumno() {
        RegularidadCurso configuracion = RegularidadCurso.builder().id(9L).curso(curso)
                .codigo("INF-201-A").asistenciaMinima(new BigDecimal("75.00"))
                .promedioMinimo(new BigDecimal("6.00")).build();
        when(cursos.findById(3L)).thenReturn(Optional.of(curso));
        when(regularidades.findByCursoId(3L)).thenReturn(Optional.of(configuracion));

        var resultado = servicio.consultar(3L);

        assertThat(resultado.cursoId()).isEqualTo(3L);
        assertThat(resultado.codigo()).isEqualTo("INF-201-A");
        assertThat(resultado.asistenciaMinima()).isEqualByComparingTo("75");
        assertThat(resultado.promedioMinimo()).isEqualByComparingTo("6");
    }

    @Test
    void actualizaUnaConfiguracionExistenteYMantieneElCodigoDelCurso() {
        RegularidadCurso configuracion = RegularidadCurso.builder().id(9L).curso(curso)
                .codigo("CODIGO-VIEJO").asistenciaMinima(new BigDecimal("75"))
                .promedioMinimo(new BigDecimal("6")).build();
        when(cursos.findById(3L)).thenReturn(Optional.of(curso));
        when(regularidades.findByCursoId(3L)).thenReturn(Optional.of(configuracion));
        when(regularidades.save(configuracion)).thenReturn(configuracion);

        var resultado = servicio.actualizar(3L,
                new GuardarCondicionesRegularidadDto(new BigDecimal("80"), new BigDecimal("7")));

        assertThat(resultado.codigo()).isEqualTo("INF-201-A");
        assertThat(resultado.asistenciaMinima()).isEqualByComparingTo("80");
        assertThat(resultado.promedioMinimo()).isEqualByComparingTo("7");
    }

    @Test
    void creaValoresInicialesCuandoElCursoNoTieneConfiguracion() {
        when(cursos.findById(3L)).thenReturn(Optional.of(curso));
        when(regularidades.findByCursoId(3L)).thenReturn(Optional.empty());
        when(regularidades.save(any(RegularidadCurso.class))).thenAnswer(invocation -> {
            RegularidadCurso guardada = invocation.getArgument(0);
            guardada.setId(10L);
            return guardada;
        });

        var resultado = servicio.consultar(3L);

        assertThat(resultado.id()).isEqualTo(10L);
        assertThat(resultado.asistenciaMinima()).isEqualByComparingTo("75");
        assertThat(resultado.promedioMinimo()).isEqualByComparingTo("6");
    }
}

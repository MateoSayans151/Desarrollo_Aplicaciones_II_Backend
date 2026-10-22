package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.EstadoInscripcionCurso;
import ar.edu.universidad.gestionacademica.Entidades.InscripcionCurso;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Repositorios.CorrelatividadRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.InscripcionCursoRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.ValidarRegularidadDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegularidadServicioTest {
    private final CursoRepositorio cursos = mock(CursoRepositorio.class);
    private final InscripcionCursoRepositorio inscripciones = mock(InscripcionCursoRepositorio.class);
    private final CorrelatividadRepositorio correlatividades = mock(CorrelatividadRepositorio.class);
    private final RegularidadServicio servicio = new RegularidadServicio(cursos, inscripciones, correlatividades);
    private final Asignatura asignatura = Asignatura.builder()
            .id(7L).codigo("INF-201").nombre("Programación II").build();
    private final Curso curso = Curso.builder().id(3L).codigo("INF-201-A").asignatura(asignatura).build();

    @BeforeEach
    void prepararInscripcion() {
        when(cursos.findById(3L)).thenReturn(Optional.of(curso));
        when(inscripciones.findByCursoIdAndAlumnoId(3L, "alu-1")).thenReturn(Optional.of(
                InscripcionCurso.builder().curso(curso).alumnoId("alu-1")
                        .estado(EstadoInscripcionCurso.CURSANDO).build()));
        when(correlatividades.findByAsignaturaId(7L)).thenReturn(List.of());
    }

    @Test
    void habilitaCuandoEstaInscriptoYCumpleTodasLasCondiciones() {
        var resultado = servicio.validar(new ValidarRegularidadDto("alu-1", 3L, 80, 7, 75, 6));

        assertThat(resultado.regular()).isTrue();
        assertThat(resultado.habilitadoParaFinal()).isTrue();
        assertThat(resultado.asignaturaNombre()).isEqualTo("Programación II");
        assertThat(resultado.cursoCodigo()).isEqualTo("INF-201-A");
    }

    @Test
    void informaTodasLasCondicionesAcademicasIncumplidas() {
        var resultado = servicio.validar(new ValidarRegularidadDto("alu-1", 3L, 60, 4, 75, 6));

        assertThat(resultado.regular()).isFalse();
        assertThat(resultado.habilitadoParaFinal()).isFalse();
        assertThat(resultado.motivos()).hasSize(2);
    }

    @Test
    void exigeQueElAlumnoEsteInscriptoEnElCurso() {
        when(inscripciones.findByCursoIdAndAlumnoId(3L, "alu-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.validar(
                new ValidarRegularidadDto("alu-2", 3L, 80, 7, 75, 6)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está inscripto");
    }

    @Test
    void mantieneRegularidadPeroNoHabilitaSiFaltaUnaCorrelativaAprobada() {
        Asignatura correlativa = Asignatura.builder()
                .id(2L).codigo("INF-101").nombre("Programación I").build();
        when(correlatividades.findByAsignaturaId(7L)).thenReturn(List.of(
                Correlatividad.builder().asignatura(asignatura).correlativa(correlativa)
                        .tipo(Correlatividad.Tipo.APROBADA).build()));
        when(inscripciones.existsByAlumnoIdAndCursoAsignaturaIdAndEstadoIn(
                eq("alu-1"), eq(2L), anyCollection())).thenReturn(false);

        var resultado = servicio.validar(new ValidarRegularidadDto("alu-1", 3L, 80, 7, 75, 6));

        assertThat(resultado.regular()).isTrue();
        assertThat(resultado.habilitadoParaFinal()).isFalse();
        assertThat(resultado.motivos()).containsExactly(
                "No tiene aprobada la correlativa INF-101 · Programación I");
    }
}

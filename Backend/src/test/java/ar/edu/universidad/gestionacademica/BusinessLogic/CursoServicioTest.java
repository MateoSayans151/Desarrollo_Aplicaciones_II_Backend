package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CursoServicioTest {
    private final CursoRepositorio cursos = mock(CursoRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final PeriodoAcademicoRepositorio periodos = mock(PeriodoAcademicoRepositorio.class);
    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final CursoDocenteRepositorio cursoDocentes = mock(CursoDocenteRepositorio.class);
    private final InscripcionCursoRepositorio inscripciones = mock(InscripcionCursoRepositorio.class);
    private final HorarioCursoRepositorio horarios = mock(HorarioCursoRepositorio.class);
    private final CorrelatividadRepositorio correlatividades = mock(CorrelatividadRepositorio.class);

    private final CursoServicio servicio = new CursoServicio(cursos, asignaturas, periodos, sedes, aulas,
            cursoDocentes, inscripciones, horarios, correlatividades);

    @Test
    void detalleDevuelveAsignaturaYPeriodoAnidados() {
        Asignatura asignatura = Asignatura.builder()
                .id(15L).codigo("DDA2").nombre("Desarrollo de Aplicaciones II").build();
        PeriodoAcademico periodo = PeriodoAcademico.builder()
                .id(8L).anio(2026).numero(2)
                .fechaInicio(LocalDate.of(2026, 8, 1)).fechaFin(LocalDate.of(2026, 11, 30))
                .inscripcionDesde(LocalDate.of(2026, 7, 1)).inscripcionHasta(LocalDate.of(2026, 7, 31))
                .build();
        Curso curso = Curso.builder()
                .id(42L).codigo("DDA2-A").asignatura(asignatura).periodo(periodo)
                .sede(Sede.builder().id(2L).build()).modalidad(ModalidadCurso.PRESENCIAL)
                .estado(EstadoCurso.EN_CURSO).cupoMaximo(45)
                .fechaInicio(LocalDate.of(2026, 8, 10)).fechaFin(LocalDate.of(2026, 11, 30))
                .build();

        when(cursos.findById(42L)).thenReturn(Optional.of(curso));
        when(cursoDocentes.findByCursoIdOrderById(42L)).thenReturn(List.of());
        when(inscripciones.findByCursoIdOrderByAlumnoId(42L)).thenReturn(List.of());
        when(horarios.findByCursoIdOrderByDiaSemanaHoraInicio(42L)).thenReturn(List.of());
        when(inscripciones.countByCursoIdAndEstadoNot(42L, EstadoInscripcionCurso.BAJA)).thenReturn(0L);

        var detalle = servicio.detalle(42L);

        assertThat(detalle.curso().asignatura().id()).isEqualTo(15L);
        assertThat(detalle.curso().asignatura().codigo()).isEqualTo("DDA2");
        assertThat(detalle.curso().asignatura().nombre()).isEqualTo("Desarrollo de Aplicaciones II");
        assertThat(detalle.curso().periodo().id()).isEqualTo(8L);
        assertThat(detalle.curso().periodo().anio()).isEqualTo(2026);
        assertThat(detalle.curso().periodo().numero()).isEqualTo(2);
        assertThat(detalle.curso().periodo().inscripcionDesde()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(detalle.curso().periodo().inscripcionHasta()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

}

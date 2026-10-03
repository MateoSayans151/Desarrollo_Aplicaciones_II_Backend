package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Integracion.Analitica.EventoResultadoPublicado;
import ar.edu.universidad.gestionacademica.Modelos.CursoDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.math.BigDecimal;
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
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    private final CursoServicio servicio = new CursoServicio(cursos, asignaturas, periodos, sedes, aulas,
            cursoDocentes, inscripciones, horarios, correlatividades, eventos);

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
        when(horarios.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(42L)).thenReturn(List.of());
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

    @Test
    void publicaEventoDeAnaliticaAlCargarUnResultadoFinal() {
        Curso curso = Curso.builder().id(42L).codigo("B1")
                .asignatura(Asignatura.builder().codigo("BDD-310").build())
                .periodo(PeriodoAcademico.builder().anio(2026).numero(2).build())
                .sede(Sede.builder().nombre("Sede Montserrat").build()).build();
        InscripcionCurso inscripcion = InscripcionCurso.builder().curso(curso).alumnoId("alumno-456")
                .estado(EstadoInscripcionCurso.CURSANDO).fechaInscripcion(LocalDate.of(2026, 8, 1)).build();
        when(inscripciones.findByCursoIdAndAlumnoId(42L, "alumno-456")).thenReturn(Optional.of(inscripcion));
        when(cursoDocentes.findByCursoIdOrderById(42L)).thenReturn(List.of(
                CursoDocente.builder().docenteId("auxiliar-2").rol(RolDocenteCurso.AUXILIAR).build(),
                CursoDocente.builder().docenteId("docente-12").rol(RolDocenteCurso.TITULAR).build()));

        servicio.actualizarInscripcion(42L, "alumno-456", new CursoDto.ActualizarInscripcionCursoDto(
                EstadoInscripcionCurso.APROBADO, new BigDecimal("8.0"), LocalDate.of(2026, 9, 30)));

        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventos).publishEvent(captor.capture());
        EventoResultadoPublicado publicado = (EventoResultadoPublicado) captor.getValue();
        assertThat(publicado.evento().eventId()).isEqualTo("acad-res-alumno-456-curso-42");
        assertThat(publicado.evento().sourceModule()).isEqualTo("academica");
        assertThat(publicado.evento().eventType()).isEqualTo("resultado.publicado");
        assertThat(publicado.evento().payload().materia()).isEqualTo("BDD-310");
        assertThat(publicado.evento().payload().docente()).isEqualTo("docente-12");
        assertThat(publicado.evento().payload().sede()).isEqualTo("Sede Montserrat");
        assertThat(publicado.evento().payload().cuatrimestre()).isEqualTo("2026-2Q");
        assertThat(publicado.evento().payload().aprobado()).isTrue();
    }

}

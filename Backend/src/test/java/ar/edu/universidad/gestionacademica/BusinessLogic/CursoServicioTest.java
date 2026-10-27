package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Integracion.Analitica.EventoResultadoPublicado;
import ar.edu.universidad.gestionacademica.Modelos.CursoDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CursoServicioTest {
    private final CursoRepositorio cursos = mock(CursoRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final PeriodoAcademicoRepositorio periodos = mock(PeriodoAcademicoRepositorio.class);
    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final AsignacionAulaRepositorio asignaciones = mock(AsignacionAulaRepositorio.class);
    private final CursoDocenteRepositorio cursoDocentes = mock(CursoDocenteRepositorio.class);
    private final InscripcionCursoRepositorio inscripciones = mock(InscripcionCursoRepositorio.class);
    private final CorrelatividadRepositorio correlatividades = mock(CorrelatividadRepositorio.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    private final CursoServicio servicio = new CursoServicio(cursos, asignaturas, periodos, sedes, aulas,
            asignaciones, cursoDocentes, inscripciones, correlatividades, eventos);

    @Test
    void crearCursoGuardaTurnoYHorasYRechazaUnRangoInvertido() {
        PeriodoAcademico periodo = PeriodoAcademico.builder().id(8L)
                .fechaInicio(LocalDate.of(2026, 8, 1)).fechaFin(LocalDate.of(2026, 11, 30)).build();
        when(periodos.findById(8L)).thenReturn(Optional.of(periodo));
        when(asignaturas.findById(15L)).thenReturn(Optional.of(Asignatura.builder().id(15L).build()));
        when(sedes.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).build()));
        when(aulas.findById(3L)).thenReturn(Optional.of(Aula.builder().id(3L)
                .sede(Sede.builder().id(2L).build()).tipo(TipoUbicacion.AULA).capacidadMaxima(50).build()));
        when(cursos.save(any(Curso.class))).thenAnswer(invocation -> {
            Curso curso = invocation.getArgument(0);
            curso.setId(42L);
            return curso;
        });
        var dto = new CursoDto.CrearCursoDto("DDA2-A", 15L, 8L, 2L, ModalidadCurso.PRESENCIAL,
                45, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 11, 30),
                TurnoCurso.TARDE, LocalTime.of(14, 0), LocalTime.of(18, 0), DayOfWeek.MONDAY, 3L);

        var creado = servicio.crear(dto);

        assertThat(creado.turno()).isEqualTo(TurnoCurso.TARDE);
        assertThat(creado.horaInicio()).isEqualTo(LocalTime.of(14, 0));
        assertThat(creado.horaFin()).isEqualTo(LocalTime.of(18, 0));
        assertThat(creado.diaSemana()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(creado.aulaId()).isEqualTo(3L);
        assertThatThrownBy(() -> servicio.crear(new CursoDto.CrearCursoDto("DDA2-B", 15L, 8L, 2L,
                ModalidadCurso.PRESENCIAL, 45, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 11, 30),
                TurnoCurso.TARDE, LocalTime.of(18, 0), LocalTime.of(14, 0), DayOfWeek.MONDAY, 3L)))
                .hasMessageContaining("hora de fin");
    }

    @Test
    void rechazaCursoCuandoCoincideConUnaAsignacionPuntual() {
        var inicio = LocalDate.of(2026, 8, 10);
        var fin = LocalDate.of(2026, 11, 30);
        PeriodoAcademico periodo = PeriodoAcademico.builder().id(8L)
                .fechaInicio(LocalDate.of(2026, 8, 1)).fechaFin(fin).build();
        when(periodos.findById(8L)).thenReturn(Optional.of(periodo));
        when(aulas.findById(3L)).thenReturn(Optional.of(Aula.builder().id(3L)
                .sede(Sede.builder().id(2L).build()).tipo(TipoUbicacion.AULA).capacidadMaxima(50).build()));
        when(asignaciones.findConflictosConCurso(3L, inicio, fin,
                LocalTime.of(14, 0), LocalTime.of(18, 0))).thenReturn(List.of(
                AsignacionAula.builder().fecha(LocalDate.of(2026, 8, 17)).build()));
        var dto = new CursoDto.CrearCursoDto("DDA2-A", 15L, 8L, 2L, ModalidadCurso.PRESENCIAL,
                45, inicio, fin, TurnoCurso.TARDE, LocalTime.of(14, 0), LocalTime.of(18, 0),
                DayOfWeek.MONDAY, 3L);

        assertThatThrownBy(() -> servicio.crear(dto))
                .isInstanceOf(ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException.class)
                .hasMessageContaining("asignación puntual");
        verify(cursos, never()).save(any());
    }

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
                .sede(Sede.builder().id(2L).nombre("Sede Centro").direccion("Av. 123").build())
                .modalidad(ModalidadCurso.PRESENCIAL)
                .estado(EstadoCurso.EN_CURSO).cupoMaximo(45)
                .fechaInicio(LocalDate.of(2026, 8, 10)).fechaFin(LocalDate.of(2026, 11, 30))
                .turno(TurnoCurso.TARDE).horaInicio(LocalTime.of(14, 0)).horaFin(LocalTime.of(18, 0))
                .diaSemana(DayOfWeek.MONDAY).aula(Aula.builder().id(3L).codigo("A-3")
                        .nombre("Laboratorio 3").build())
                .build();

        when(cursos.findById(42L)).thenReturn(Optional.of(curso));
        when(cursoDocentes.findByCursoIdOrderById(42L)).thenReturn(List.of());
        when(inscripciones.findByCursoIdOrderByAlumnoId(42L)).thenReturn(List.of());
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
        assertThat(detalle.curso().turno()).isEqualTo(TurnoCurso.TARDE);
        assertThat(detalle.curso().horaInicio()).isEqualTo(LocalTime.of(14, 0));
        assertThat(detalle.curso().horaFin()).isEqualTo(LocalTime.of(18, 0));
        assertThat(detalle.curso().diaSemana()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(detalle.curso().aulaId()).isEqualTo(3L);
        assertThat(detalle.curso().sedeNombre()).isEqualTo("Sede Centro");
        assertThat(detalle.curso().aulaNombre()).isEqualTo("Laboratorio 3");
    }

    @Test
    void listaDocentesYAlumnosDelCurso() {
        Curso curso = Curso.builder().id(42L).build();
        when(cursos.findById(42L)).thenReturn(Optional.of(curso));
        when(cursoDocentes.findByCursoIdOrderById(42L)).thenReturn(List.of(
                CursoDocente.builder().id(3L).docenteId("docente-12").rol(RolDocenteCurso.TITULAR).build()));
        when(inscripciones.findByCursoIdOrderByAlumnoId(42L)).thenReturn(List.of(
                InscripcionCurso.builder().id(7L).alumnoId("alumno-456")
                        .fechaInscripcion(LocalDate.of(2026, 7, 15))
                        .estado(EstadoInscripcionCurso.CURSANDO).build()));

        var docentes = servicio.listarDocentes(42L);
        var alumnos = servicio.listarAlumnos(42L);

        assertThat(docentes).singleElement().satisfies(docente -> {
            assertThat(docente.docenteId()).isEqualTo("docente-12");
            assertThat(docente.rol()).isEqualTo(RolDocenteCurso.TITULAR);
        });
        assertThat(alumnos).singleElement().satisfies(alumno -> {
            assertThat(alumno.alumnoId()).isEqualTo("alumno-456");
            assertThat(alumno.estado()).isEqualTo(EstadoInscripcionCurso.CURSANDO);
        });
    }

    @Test
    void listaLasInscripcionesDeUnAlumnoConCursoAsignaturaYPeriodo() {
        Asignatura asignatura = Asignatura.builder().id(15L).codigo("DDA2")
                .nombre("Desarrollo de Aplicaciones II").build();
        PeriodoAcademico periodo = PeriodoAcademico.builder().id(8L).anio(2026).numero(2)
                .fechaInicio(LocalDate.of(2026, 8, 1)).fechaFin(LocalDate.of(2026, 11, 30))
                .inscripcionDesde(LocalDate.of(2026, 7, 1)).inscripcionHasta(LocalDate.of(2026, 7, 31)).build();
        Curso curso = Curso.builder().id(42L).codigo("DDA2-A").asignatura(asignatura).periodo(periodo)
                .sede(Sede.builder().id(2L).build()).modalidad(ModalidadCurso.PRESENCIAL)
                .estado(EstadoCurso.EN_CURSO).cupoMaximo(45).fechaInicio(LocalDate.of(2026, 8, 10))
                .fechaFin(LocalDate.of(2026, 11, 30)).build();
        InscripcionCurso inscripcion = InscripcionCurso.builder().id(71L).curso(curso).alumnoId("alumno-456")
                .fechaInscripcion(LocalDate.of(2026, 7, 15)).estado(EstadoInscripcionCurso.CURSANDO).build();
        when(inscripciones.findByAlumnoIdAndCursoPeriodoIdOrderByCursoCodigoAsc("alumno-456", 8L))
                .thenReturn(List.of(inscripcion));

        var resultado = servicio.listarInscripcionesAlumno(" alumno-456 ", 8L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).alumnoId()).isEqualTo("alumno-456");
        assertThat(resultado.get(0).curso().id()).isEqualTo(42L);
        assertThat(resultado.get(0).curso().asignatura().nombre()).isEqualTo("Desarrollo de Aplicaciones II");
        assertThat(resultado.get(0).curso().periodo().id()).isEqualTo(8L);
        verify(inscripciones).findByAlumnoIdAndCursoPeriodoIdOrderByCursoCodigoAsc("alumno-456", 8L);
    }

    @Test
    void listaTodasLasInscripcionesDelAlumnoCuandoNoSeIndicaPeriodo() {
        when(inscripciones.findByAlumnoIdOrderByCursoPeriodoIdDescCursoCodigoAsc("alumno-456"))
                .thenReturn(List.of());

        var resultado = servicio.listarInscripcionesAlumno("alumno-456", null);

        assertThat(resultado).isEmpty();
        verify(inscripciones).findByAlumnoIdOrderByCursoPeriodoIdDescCursoCodigoAsc("alumno-456");
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

    @Test
    void listaNotasDelCursoIncluyendoAlumnosSinNota() {
        when(cursos.findById(42L)).thenReturn(Optional.of(Curso.builder().id(42L).build()));
        when(inscripciones.findByCursoIdOrderByAlumnoId(42L)).thenReturn(List.of(
                InscripcionCurso.builder().alumnoId("core-alumno-1").estado(EstadoInscripcionCurso.CURSANDO).build(),
                InscripcionCurso.builder().alumnoId("core-alumno-2").estado(EstadoInscripcionCurso.APROBADO)
                        .notaFinal(new BigDecimal("8.50")).fechaResultado(LocalDate.of(2026, 10, 12)).build()));

        var notas = servicio.listarNotas(42L);

        assertThat(notas).hasSize(2);
        assertThat(notas.get(0).alumnoId()).isEqualTo("core-alumno-1");
        assertThat(notas.get(0).notaFinal()).isNull();
        assertThat(notas.get(1).notaFinal()).isEqualByComparingTo("8.50");
    }

    @Test
    void cargaNotaFinalEnInscripcionExistenteYPublicaResultadoSoloCuandoCambia() {
        Curso curso = Curso.builder().id(42L).codigo("DDA2-A")
                .asignatura(Asignatura.builder().codigo("DDA2").build())
                .periodo(PeriodoAcademico.builder().anio(2026).numero(2).build())
                .sede(Sede.builder().nombre("Sede Centro").build()).build();
        InscripcionCurso inscripcion = InscripcionCurso.builder().curso(curso).alumnoId("core-alumno-1")
                .estado(EstadoInscripcionCurso.CURSANDO).build();
        when(inscripciones.findByCursoIdAndAlumnoId(42L, "core-alumno-1"))
                .thenReturn(Optional.of(inscripcion));
        when(cursoDocentes.findByCursoIdOrderById(42L)).thenReturn(List.of());
        var dto = new CursoDto.CargarNotaFinalDto(new BigDecimal("8.50"), EstadoInscripcionCurso.APROBADO,
                LocalDate.of(2026, 10, 12));

        var nota = servicio.cargarNota(42L, "core-alumno-1", dto);
        servicio.cargarNota(42L, "core-alumno-1", dto);

        assertThat(nota.alumnoId()).isEqualTo("core-alumno-1");
        assertThat(nota.notaFinal()).isEqualByComparingTo("8.50");
        assertThat(inscripcion.getEstado()).isEqualTo(EstadoInscripcionCurso.APROBADO);
        verify(eventos, times(1)).publishEvent(any(EventoResultadoPublicado.class));
    }

    @Test
    void rechazaEstadoNoFinalAlCargarNota() {
        var dto = new CursoDto.CargarNotaFinalDto(new BigDecimal("8.50"), EstadoInscripcionCurso.CURSANDO,
                LocalDate.of(2026, 10, 12));

        assertThatThrownBy(() -> servicio.cargarNota(42L, "core-alumno-1", dto))
                .hasMessageContaining("APROBADO o DESAPROBADO");
        verifyNoInteractions(inscripciones);
    }

}

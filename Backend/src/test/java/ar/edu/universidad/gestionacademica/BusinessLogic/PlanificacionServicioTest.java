package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import ar.edu.universidad.gestionacademica.Entidades.AsignacionAula;
import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.EstadoCurso;
import ar.edu.universidad.gestionacademica.Entidades.Sede;
import ar.edu.universidad.gestionacademica.Entidades.TipoAsignacion;
import ar.edu.universidad.gestionacademica.Entidades.TurnoExamen;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Integracion.Backoffice.PropiedadesBackoffice;
import ar.edu.universidad.gestionacademica.Integracion.Core.PublicadorEventosCore;
import ar.edu.universidad.gestionacademica.Modelos.PlanificacionDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlanificacionServicioTest {
    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final AsignacionAulaRepositorio asignaciones = mock(AsignacionAulaRepositorio.class);
    private final CursoRepositorio cursos = mock(CursoRepositorio.class);
    private final PlanificacionServicio servicio = new PlanificacionServicio(sedes, aulas,
            asignaturas, asignaciones, cursos,
            mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class),
            new PropiedadesBackoffice(false, null, null, null, 0, null), mock(PublicadorEventosCore.class));

    @Test
    void guardaYDevuelveElNombreDelAula() {
        when(sedes.save(any(Sede.class))).thenAnswer(invocacion -> {
            Sede sede = invocacion.getArgument(0);
            sede.setId(2L);
            return sede;
        });

        var sede = servicio.crearSede(new PlanificacionDto.CrearSedeDto("Sede Centro", "Av. 123"));

        assertThat(sede.nombre()).isEqualTo("Sede Centro");
        when(sedes.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).build()));
        when(aulas.save(any(Aula.class))).thenAnswer(invocacion -> {
            Aula aula = invocacion.getArgument(0);
            aula.setId(4L);
            return aula;
        });

        var aula = servicio.crearAula(2L, new PlanificacionDto.CrearAulaDto(
                "A-4", 45, " Laboratorio 4 "));

        assertThat(aula.nombre()).isEqualTo("Laboratorio 4");
    }

    @Test
    void conservaCodigoComoNombreCuandoAulaNoTraeNombre() {
        when(sedes.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).build()));
        when(aulas.save(any(Aula.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var aula = servicio.crearAula(2L, new PlanificacionDto.CrearAulaDto("A-4", 45, null));

        assertThat(aula.nombre()).isEqualTo("A-4");
    }

    @Test
    void listaAsignacionesYAgendaDeUnaAsignatura() {
        var aula = Aula.builder().id(4L).build();
        var asignatura = Asignatura.builder().id(8L).build();
        var fecha = LocalDate.of(2026, 10, 21);
        var asignacion = AsignacionAula.builder().id(12L).aula(aula).asignatura(asignatura)
                .fecha(fecha).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0))
                .cantidadEstudiantes(30).build();
        when(asignaciones.findAllByOrderByFechaAscHoraInicioAsc()).thenReturn(List.of(asignacion));
        when(asignaturas.existsById(8L)).thenReturn(true);
        when(asignaciones.findByAsignaturaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
                8L, fecha, fecha.plusDays(6))).thenReturn(List.of(asignacion));

        assertThat(servicio.listarAsignaciones()).singleElement().satisfies(item -> {
            assertThat(item.aulaId()).isEqualTo(4L);
            assertThat(item.asignaturaId()).isEqualTo(8L);
            assertThat(item.fecha()).isEqualTo(fecha);
        });
        assertThat(servicio.agendaAsignatura(8L, fecha, fecha.plusDays(6)))
                .extracting(PlanificacionDto.AsignacionRespuestaDto::id).containsExactly(12L);
    }

    @Test
    void rechazaRangoInvertidoEnAgenda() {
        when(asignaturas.existsById(8L)).thenReturn(true);
        var fecha = LocalDate.of(2026, 10, 21);

        assertThatThrownBy(() -> servicio.agendaAsignatura(8L, fecha.plusDays(1), fecha))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(asignaciones);
    }

    @Test
    void devuelveLaAgendaDeUnAulaParaUnRangoDeFechas() {
        var aula = Aula.builder().id(4L).build();
        var asignatura = Asignatura.builder().id(8L).build();
        var desde = LocalDate.of(2026, 10, 1);
        var hasta = LocalDate.of(2026, 10, 31);
        var asignacion = AsignacionAula.builder().id(12L).aula(aula).asignatura(asignatura)
                .fecha(LocalDate.of(2026, 10, 21)).horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0)).cantidadEstudiantes(30).build();
        when(aulas.existsById(4L)).thenReturn(true);
        when(asignaciones.findByAulaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(4L, desde, hasta))
                .thenReturn(List.of(asignacion));

        assertThat(servicio.agendaAula(4L, desde, hasta))
                .extracting(PlanificacionDto.OcupacionAulaDto::referenciaId).containsExactly(12L);
    }

    @Test
    void combinaAsignacionesPuntualesYOcurrenciasSemanalesDeCursosEnLaAgenda() {
        var aula = Aula.builder().id(4L).build();
        var asignatura = Asignatura.builder().id(8L).codigo("DDA2")
                .nombre("Desarrollo de Aplicaciones II").build();
        var desde = LocalDate.of(2026, 10, 1);
        var hasta = LocalDate.of(2026, 10, 31);
        var asignacion = AsignacionAula.builder().id(12L).aula(aula).asignatura(asignatura)
                .fecha(LocalDate.of(2026, 10, 7)).horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(9, 0)).cantidadEstudiantes(20).build();
        var curso = Curso.builder().id(42L).codigo("DDA2-A").aula(aula).asignatura(asignatura)
                .estado(EstadoCurso.EN_CURSO).fechaInicio(LocalDate.of(2026, 10, 5))
                .fechaFin(LocalDate.of(2026, 10, 26)).diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(10, 0)).horaFin(LocalTime.of(12, 0)).build();
        when(aulas.existsById(4L)).thenReturn(true);
        when(asignaciones.findByAulaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(4L, desde, hasta))
                .thenReturn(List.of(asignacion));
        when(cursos.findOcupacionesEnRango(4L, desde, hasta, EstadoCurso.CANCELADO))
                .thenReturn(List.of(curso));

        var agenda = servicio.agendaAula(4L, desde, hasta);

        assertThat(agenda).hasSize(5);
        assertThat(agenda).filteredOn(item -> item.tipo() == PlanificacionDto.TipoOcupacionAula.CURSO)
                .extracting(PlanificacionDto.OcupacionAulaDto::fecha)
                .containsExactly(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26));
        assertThat(agenda).filteredOn(item -> item.tipo() == PlanificacionDto.TipoOcupacionAula.ASIGNACION)
                .singleElement().satisfies(item -> assertThat(item.referenciaId()).isEqualTo(12L));
    }

    @Test
    void rechazaAsignacionPuntualCuandoElAulaEstaOcupadaPorUnCurso() {
        var fecha = LocalDate.now().plusDays(1);
        var aula = Aula.builder().id(4L).tipo(ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion.AULA)
                .capacidadMaxima(40).build();
        when(aulas.findById(4L)).thenReturn(Optional.of(aula));
        when(asignaturas.findById(8L)).thenReturn(Optional.of(Asignatura.builder().id(8L).plan(ar.edu.universidad.gestionacademica.Entidades.PlanEstudio.builder().estado(ar.edu.universidad.gestionacademica.Entidades.EstadoPlanEstudio.ACTIVO).carrera(ar.edu.universidad.gestionacademica.Entidades.Carrera.builder().build()).build()).build()));
        when(cursos.countOcupacionesEnFecha(4L, fecha.getDayOfWeek(), fecha,
                LocalTime.of(10, 0), LocalTime.of(12, 0), EstadoCurso.CANCELADO)).thenReturn(1L);
        var dto = new PlanificacionDto.CrearAsignacionDto(4L, 8L, fecha,
                LocalTime.of(10, 0), LocalTime.of(12, 0), 30);

        assertThatThrownBy(() -> servicio.asignarAula(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ocupada por un curso");
        verify(asignaciones, never()).save(any());
    }

    @Test
    void rechazaReservarUnAulaEnMantenimientoEnBackoffice() {
        var aula = Aula.builder().id(4L).tipo(ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion.AULA)
                .capacidadMaxima(40).estado("MAINTENANCE").sede(Sede.builder().id(2L).build()).build();
        when(aulas.findById(4L)).thenReturn(Optional.of(aula));
        var dto = new PlanificacionDto.CrearAsignacionDto(4L, 8L, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(12, 0), 30);

        assertThatThrownBy(() -> servicio.asignarAula(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("MAINTENANCE");
        verify(asignaciones, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unExamenSePublicaEnCoreConLosIdsDeBackoffice() {
        var eventos = mock(PublicadorEventosCore.class);
        var turnos = mock(TurnoExamenRepositorio.class);
        var conEventos = new PlanificacionServicio(sedes, aulas, asignaturas, asignaciones, cursos,
                mock(PeriodoAcademicoRepositorio.class), turnos,
                new PropiedadesBackoffice(false, null, null, null, 0, null), eventos);
        when(eventos.habilitado()).thenReturn(true);
        var fecha = LocalDate.of(2099, 12, 5);
        UUID site = UUID.fromString("3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90");
        UUID location = UUID.fromString("c41a77b2-0000-0000-0000-000000000305");
        when(aulas.findById(4L)).thenReturn(Optional.of(aulaDeBackoffice(site, location)));
        when(asignaturas.findById(8L)).thenReturn(Optional.of(asignaturaPlanificable()));
        when(turnos.findFirstByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(fecha, fecha))
                .thenReturn(Optional.of(TurnoExamen.builder().id(3L).nombre("Diciembre 2099").build()));
        when(asignaciones.save(any(AsignacionAula.class))).thenAnswer(inv -> {
            AsignacionAula guardada = inv.getArgument(0);
            guardada.setId(21L);
            return guardada;
        });

        var respuesta = conEventos.asignarAula(new PlanificacionDto.CrearAsignacionDto(4L, 8L, fecha,
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, TipoAsignacion.EXAMEN));

        assertThat(respuesta.tipo()).isEqualTo(TipoAsignacion.EXAMEN);
        ArgumentCaptor<Map<String, Object>> data = ArgumentCaptor.forClass(Map.class);
        verify(eventos).registrar(eq(PublicadorEventosCore.EXAMEN_PROGRAMADO), eq("exam/21"), data.capture());
        assertThat(data.getValue())
                .containsEntry("examId", "21")
                .containsEntry("subjectId", "8")
                .containsEntry("startAt", "2099-12-05T09:00:00-03:00")
                .containsEntry("endAt", "2099-12-05T12:00:00-03:00")
                .containsEntry("siteId", site.toString())
                .containsEntry("locationId", location.toString())
                .containsEntry("examTermId", "3");
    }

    @Test
    void unaClaseNoSePublicaComoExamen() {
        var eventos = mock(PublicadorEventosCore.class);
        var conEventos = new PlanificacionServicio(sedes, aulas, asignaturas, asignaciones, cursos,
                mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class),
                new PropiedadesBackoffice(false, null, null, null, 0, null), eventos);
        when(eventos.habilitado()).thenReturn(true);
        when(aulas.findById(4L)).thenReturn(Optional.of(aulaDeBackoffice(UUID.randomUUID(), UUID.randomUUID())));
        when(asignaturas.findById(8L)).thenReturn(Optional.of(asignaturaPlanificable()));
        when(asignaciones.save(any(AsignacionAula.class))).thenAnswer(inv -> inv.getArgument(0));

        var respuesta = conEventos.asignarAula(new PlanificacionDto.CrearAsignacionDto(4L, 8L,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0), 30));

        assertThat(respuesta.tipo()).isEqualTo(TipoAsignacion.CLASE);
        verify(eventos, never()).registrar(any(), any(), any());
    }

    @Test
    void unExamenEnUnAulaLocalSeRechazaSiSePublicaEnCore() {
        var eventos = mock(PublicadorEventosCore.class);
        var conEventos = new PlanificacionServicio(sedes, aulas, asignaturas, asignaciones, cursos,
                mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class),
                new PropiedadesBackoffice(false, null, null, null, 0, null), eventos);
        when(eventos.habilitado()).thenReturn(true);
        when(aulas.findById(4L)).thenReturn(Optional.of(Aula.builder().id(4L)
                .tipo(ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion.AULA).capacidadMaxima(40)
                .sede(Sede.builder().id(2L).build()).build()));
        when(asignaturas.findById(8L)).thenReturn(Optional.of(asignaturaPlanificable()));

        assertThatThrownBy(() -> conEventos.asignarAula(new PlanificacionDto.CrearAsignacionDto(4L, 8L,
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0), 30, TipoAsignacion.EXAMEN)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Backoffice");
        verify(asignaciones, never()).save(any());
    }

    private static Aula aulaDeBackoffice(UUID site, UUID location) {
        return Aula.builder().id(4L).nombre("Aula 305").backofficeLocationId(location)
                .tipo(ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion.AULA).capacidadMaxima(40)
                .sede(Sede.builder().id(2L).backofficeSiteId(site).build()).build();
    }

    private static Asignatura asignaturaPlanificable() {
        return Asignatura.builder().id(8L).codigo("PROG1").nombre("Programación I")
                .plan(ar.edu.universidad.gestionacademica.Entidades.PlanEstudio.builder()
                        .estado(ar.edu.universidad.gestionacademica.Entidades.EstadoPlanEstudio.ACTIVO)
                        .carrera(ar.edu.universidad.gestionacademica.Entidades.Carrera.builder().build()).build())
                .build();
    }

    @Test
    void conBackofficeActivoNoSeCreanSedesNiAulasLocales() {
        var integrado = new PlanificacionServicio(sedes, aulas, asignaturas, asignaciones, cursos,
                mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class),
                new PropiedadesBackoffice(true, "https://bo", null, null, 100, null), mock(PublicadorEventosCore.class));

        assertThatThrownBy(() -> integrado.crearSede(new PlanificacionDto.CrearSedeDto("Sede", "Av. 1")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Backoffice");
        assertThatThrownBy(() -> integrado.crearAula(2L, new PlanificacionDto.CrearAulaDto("A-1", 30, null)))
                .isInstanceOf(ReglaNegocioException.class);
        verify(sedes, never()).save(any());
        verify(aulas, never()).save(any());
    }

    @Test
    void rechazaUnRangoInvertidoEnLaAgendaDelAula() {
        var desde = LocalDate.of(2026, 10, 1);
        when(aulas.existsById(4L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.agendaAula(4L, desde.plusDays(1), desde))
                .isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(asignaciones);
    }
}

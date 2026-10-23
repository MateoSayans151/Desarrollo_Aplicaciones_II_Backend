package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import ar.edu.universidad.gestionacademica.Entidades.AsignacionAula;
import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import ar.edu.universidad.gestionacademica.Entidades.Sede;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Modelos.PlanificacionDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanificacionServicioTest {
    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final AsignacionAulaRepositorio asignaciones = mock(AsignacionAulaRepositorio.class);
    private final PlanificacionServicio servicio = new PlanificacionServicio(sedes, aulas,
            asignaturas, asignaciones,
            mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class));

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
                .extracting(PlanificacionDto.AsignacionRespuestaDto::id).containsExactly(12L);
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

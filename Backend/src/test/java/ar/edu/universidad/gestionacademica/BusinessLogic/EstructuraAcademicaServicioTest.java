package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Carrera;
import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import ar.edu.universidad.gestionacademica.Entidades.EstadoPlanEstudio;
import ar.edu.universidad.gestionacademica.Entidades.PlanEstudio;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.CrearCorrelatividadDto;
import ar.edu.universidad.gestionacademica.Repositorios.AsignaturaRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CarreraRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CorrelatividadRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.PlanEstudioRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.time.LocalDate;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.CrearCarreraDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EstructuraAcademicaServicioTest {
    private final CarreraRepositorio carreras = mock(CarreraRepositorio.class);
    private final PlanEstudioRepositorio planes = mock(PlanEstudioRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final CorrelatividadRepositorio correlatividades = mock(CorrelatividadRepositorio.class);
    private final EstructuraAcademicaServicio servicio = new EstructuraAcademicaServicio(
            carreras, planes, asignaturas, correlatividades);

    @Test
    void creaLaCarreraSinDuracionInicial() {
        when(carreras.save(any(Carrera.class))).thenAnswer(invocacion -> {
            Carrera carrera = invocacion.getArgument(0);
            carrera.setId(4L);
            return carrera;
        });
        var creada = servicio.crearCarrera(new CrearCarreraDto(
                "INF-001", "Ingeniería Informática", "Ingeniería", "Ingeniero/a"));

        assertThat(creada.duracion()).isEqualTo("0 años");
    }

    @Test
    void calculaUnAnioPorCadaDiezMaterias() {
        Carrera carrera = carreraDePrueba();
        carrera.setDuracion("6 años");
        when(carreras.findAll()).thenReturn(List.of(carrera));

        assertThat(servicio.listarCarreras()).singleElement()
                .extracting(respuesta -> respuesta.duracion()).isEqualTo("6 años");
    }

    @Test
    void redondeaLaDuracionAlCuatrimestreNecesario() {
        Carrera carrera = carreraDePrueba();
        carrera.setDuracion("1,5 años");
        when(carreras.findAll()).thenReturn(List.of(carrera));

        assertThat(servicio.listarCarreras()).singleElement()
                .extracting(respuesta -> respuesta.duracion()).isEqualTo("1,5 años");
    }

    @Test
    void creaElPlanPlanificadoSinAsignaturas() {
        Carrera carrera = carreraDePrueba();
        when(carreras.findById(4L)).thenReturn(java.util.Optional.of(carrera));
        when(planes.save(any(PlanEstudio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = servicio.crearPlan(4L, new ar.edu.universidad.gestionacademica.Modelos
                .EstructuraAcademicaDto.CrearPlanDto("INF-002", "Plan de estudio 2027", LocalDate.of(2027, 1, 1)));

        assertThat(respuesta.estado()).isEqualTo(EstadoPlanEstudio.PLANIFICADO);
        assertThat(respuesta.cantidadAsignaturas()).isZero();
    }

    @Test
    void impideCambiarElEstadoDeUnPlanActivo() {
        PlanEstudio plan = PlanEstudio.builder().id(2L).codigo("INF-002").nombre("Plan 2027")
                .vigenciaDesde(LocalDate.of(2027, 1, 1)).estado(EstadoPlanEstudio.ACTIVO)
                .carrera(carreraDePrueba()).build();
        when(planes.findById(2L)).thenReturn(java.util.Optional.of(plan));

        assertThatThrownBy(() -> servicio.actualizarPlan(2L,
                new ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.ActualizarPlanDto(
                        "INF-002", "Plan 2027", LocalDate.of(2027, 1, 1), EstadoPlanEstudio.PLANIFICADO)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Un plan activo no puede cambiar de estado");
    }

    @Test
    void soloEliminaPlanesPlanificados() {
        PlanEstudio activo = PlanEstudio.builder().id(2L).estado(EstadoPlanEstudio.ACTIVO).build();
        when(planes.findById(2L)).thenReturn(java.util.Optional.of(activo));

        assertThatThrownBy(() -> servicio.eliminarPlan(2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden eliminar planes en estado PLANIFICADO");
        verify(planes, never()).delete(any());
    }

    @Test
    void eliminaLasAsignaturasDelPlanPlanificadoAntesDeEliminarlo() {
        Carrera carrera = carreraDePrueba();
        PlanEstudio planificado = PlanEstudio.builder().id(2L).estado(EstadoPlanEstudio.PLANIFICADO)
                .carrera(carrera).build();
        when(planes.findById(2L)).thenReturn(java.util.Optional.of(planificado));
        when(planes.findByCarreraIdOrderByIdDesc(4L)).thenReturn(List.of());

        servicio.eliminarPlan(2L);

        var orden = inOrder(correlatividades, asignaturas, planes);
        orden.verify(correlatividades).deleteByPlanId(2L);
        orden.verify(asignaturas).deleteByPlanId(2L);
        orden.verify(planes).delete(planificado);
        assertThat(carrera.getDuracion()).isEqualTo("0 años");
    }

    @Test
    void permiteAsignarMateriasAUnPlanPlanificadoYActualizaLosContadores() {
        Carrera carrera = carreraDePrueba();
        PlanEstudio plan = PlanEstudio.builder().id(2L).codigo("INF-002").nombre("Plan 2027")
                .vigenciaDesde(LocalDate.of(2027, 1, 1)).estado(EstadoPlanEstudio.PLANIFICADO)
                .cantidadAsignaturas(0).carrera(carrera).build();
        when(planes.findById(2L)).thenReturn(java.util.Optional.of(plan));
        when(asignaturas.saveAndFlush(any(Asignatura.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(asignaturas.countByPlanId(2L)).thenReturn(14L);
        when(planes.findByCarreraIdOrderByIdDesc(4L)).thenReturn(List.of(plan));

        servicio.crearAsignatura(2L,
                new ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.CrearAsignaturaDto(
                        "INF-014", "Arquitectura", 2, 6, 64));

        assertThat(plan.getCantidadAsignaturas()).isEqualTo(14);
        assertThat(carrera.getDuracion()).isEqualTo("1,5 años");
    }

    @Test
    void informaElUltimoPlanCreadoQueEsteActivo() {
        Carrera carrera = carreraDePrueba();
        PlanEstudio activo = PlanEstudio.builder().id(2L).estado(EstadoPlanEstudio.ACTIVO)
                .carrera(carrera).build();
        when(carreras.findAll()).thenReturn(List.of(carrera));
        when(planes.findFirstByCarreraIdAndEstadoOrderByIdDesc(4L, EstadoPlanEstudio.ACTIVO))
                .thenReturn(java.util.Optional.of(activo));

        assertThat(servicio.listarCarreras()).singleElement()
                .extracting(respuesta -> respuesta.ultimoPlanActivo().id()).isEqualTo(2L);
    }

    @Test
    void agrupaLasCorrelativasPorCondicionDeRegularidad() {
        Asignatura destino = Asignatura.builder().id(15L).build();
        when(asignaturas.findById(15L)).thenReturn(java.util.Optional.of(destino));
        when(correlatividades.findByAsignaturaId(15L)).thenReturn(List.of(
                Correlatividad.builder().asignatura(destino)
                        .correlativa(Asignatura.builder().id(13L).codigo("MAT-013")
                                .nombre("Matemática II").build())
                        .tipo(Correlatividad.Tipo.REGULAR).build(),
                Correlatividad.builder().asignatura(destino)
                        .correlativa(Asignatura.builder().id(12L).codigo("PRG-012")
                                .nombre("Programación II").build())
                        .tipo(Correlatividad.Tipo.APROBADA).build(),
                Correlatividad.builder().asignatura(destino)
                        .correlativa(Asignatura.builder().id(11L).codigo("ALG-011")
                                .nombre("Álgebra").build())
                        .tipo(Correlatividad.Tipo.APROBADA).build()));

        var respuesta = servicio.listarCorrelatividades(15L);

        assertThat(respuesta.asignaturaId()).isEqualTo(15L);
        assertThat(respuesta.condiciones()).hasSize(2);
        assertThat(respuesta.condiciones().get(0).tipo()).isEqualTo(Correlatividad.Tipo.REGULAR);
        assertThat(respuesta.condiciones().get(0).materiasCorrelativas()).singleElement().satisfies(materia -> {
            assertThat(materia.id()).isEqualTo(13L);
            assertThat(materia.codigo()).isEqualTo("MAT-013");
            assertThat(materia.nombre()).isEqualTo("Matemática II");
        });
        assertThat(respuesta.condiciones().get(1).tipo()).isEqualTo(Correlatividad.Tipo.APROBADA);
        assertThat(respuesta.condiciones().get(1).materiasCorrelativas())
                .extracting(materia -> materia.id(), materia -> materia.codigo(), materia -> materia.nombre())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(11L, "ALG-011", "Álgebra"),
                        org.assertj.core.groups.Tuple.tuple(12L, "PRG-012", "Programación II"));
    }

    @Test
    void rechazaUnaMateriaYaConfiguradaAunqueSeIntenteCambiarElTipo() {
        PlanEstudio plan = PlanEstudio.builder().id(2L).build();
        when(asignaturas.findById(15L)).thenReturn(java.util.Optional.of(
                Asignatura.builder().id(15L).plan(plan).build()));
        when(asignaturas.findById(13L)).thenReturn(java.util.Optional.of(
                Asignatura.builder().id(13L).plan(plan).build()));
        when(correlatividades.existsByAsignaturaIdAndCorrelativaId(15L, 13L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.agregarCorrelatividad(15L,
                new CrearCorrelatividadDto(13L, Correlatividad.Tipo.APROBADA)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La materia ya está configurada como correlativa para esta asignatura");
        verify(correlatividades, never()).saveAndFlush(any());
    }

    @Test
    void convierteUnaColisionConcurrenteEnErrorDeNegocio() {
        PlanEstudio plan = PlanEstudio.builder().id(2L).build();
        when(asignaturas.findById(15L)).thenReturn(java.util.Optional.of(
                Asignatura.builder().id(15L).plan(plan).build()));
        when(asignaturas.findById(13L)).thenReturn(java.util.Optional.of(
                Asignatura.builder().id(13L).plan(plan).build()));
        when(correlatividades.saveAndFlush(any(Correlatividad.class)))
                .thenThrow(new DataIntegrityViolationException("duplicada"));

        assertThatThrownBy(() -> servicio.agregarCorrelatividad(15L,
                new CrearCorrelatividadDto(13L, Correlatividad.Tipo.REGULAR)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La materia ya está configurada como correlativa para esta asignatura");
    }

    private Carrera carreraDePrueba() {
        return Carrera.builder().id(4L).codigo("INF-001").nombre("Ingeniería Informática")
                .facultad("Ingeniería").titulo("Ingeniero/a").build();
    }
}

package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EstructuraAcademicaServicio {
    private final CarreraRepositorio carreras;
    private final PlanEstudioRepositorio planes;
    private final AsignaturaRepositorio asignaturas;
    private final CorrelatividadRepositorio correlatividades;

    public CarreraRespuestaDto crearCarrera(CrearCarreraDto dto) {
        if (carreras.existsByCodigoIgnoreCase(dto.codigo())) {
            throw new ReglaNegocioException("Ya existe una carrera con el codigo indicado");
        }
        return carrera(carreras.save(Carrera.builder().codigo(dto.codigo()).nombre(dto.nombre()).build()));
    }

    @Transactional(readOnly = true)
    public List<CarreraRespuestaDto> listarCarreras() {
        return carreras.findAll().stream().map(this::carrera).toList();
    }

    public CarreraRespuestaDto actualizarCarrera(Long id, CrearCarreraDto dto) {
        Carrera entidad = buscarCarrera(id);
        entidad.setCodigo(dto.codigo());
        entidad.setNombre(dto.nombre());
        return carrera(entidad);
    }

    public void eliminarCarrera(Long id) {
        Carrera entidad = buscarCarrera(id);
        entidad.setActiva(false);
    }

    public PlanRespuestaDto crearPlan(Long carreraId, CrearPlanDto dto) {
        validarRango(dto.vigenciaDesde(), dto.vigenciaHasta(), "vigencia del plan");
        PlanEstudio plan = PlanEstudio.builder().version(dto.version()).vigenciaDesde(dto.vigenciaDesde())
                .vigenciaHasta(dto.vigenciaHasta()).carrera(buscarCarrera(carreraId)).build();
        return plan(planes.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanRespuestaDto> listarPlanes(Long carreraId) {
        buscarCarrera(carreraId);
        return planes.findByCarreraIdOrderByVigenciaDesdeDesc(carreraId).stream().map(this::plan).toList();
    }

    public AsignaturaRespuestaDto crearAsignatura(Long planId, CrearAsignaturaDto dto) {
        Asignatura asignatura = Asignatura.builder().codigo(dto.codigo()).nombre(dto.nombre())
                .cuatrimestreSugerido(dto.cuatrimestreSugerido()).cargaHoraria(dto.cargaHoraria())
                .plan(buscarPlan(planId)).build();
        return asignatura(asignaturas.save(asignatura));
    }

    @Transactional(readOnly = true)
    public List<AsignaturaRespuestaDto> listarAsignaturas(Long planId) {
        buscarPlan(planId);
        return asignaturas.findByPlanIdOrderByCuatrimestreSugeridoAscNombreAsc(planId)
                .stream().map(this::asignatura).toList();
    }

    public CorrelatividadRespuestaDto agregarCorrelatividad(Long asignaturaId, CrearCorrelatividadDto dto) {
        if (asignaturaId.equals(dto.correlativaId())) {
            throw new ReglaNegocioException("Una asignatura no puede ser correlativa de si misma");
        }
        Asignatura destino = buscarAsignatura(asignaturaId);
        Asignatura requerida = buscarAsignatura(dto.correlativaId());
        if (!destino.getPlan().getId().equals(requerida.getPlan().getId())) {
            throw new ReglaNegocioException("Las asignaturas deben pertenecer al mismo plan de estudio");
        }
        Correlatividad entidad = correlatividades.save(Correlatividad.builder().asignatura(destino)
                .correlativa(requerida).tipo(dto.tipo()).build());
        return correlatividad(entidad);
    }

    @Transactional(readOnly = true)
    public List<CorrelatividadRespuestaDto> listarCorrelatividades(Long asignaturaId) {
        buscarAsignatura(asignaturaId);
        return correlatividades.findByAsignaturaId(asignaturaId).stream().map(this::correlatividad).toList();
    }

    private Carrera buscarCarrera(Long id) {
        return carreras.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Carrera no encontrada"));
    }
    private PlanEstudio buscarPlan(Long id) {
        return planes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado"));
    }
    private Asignatura buscarAsignatura(Long id) {
        return asignaturas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Asignatura no encontrada"));
    }
    private void validarRango(java.time.LocalDate inicio, java.time.LocalDate fin, String nombre) {
        if (fin != null && !fin.isAfter(inicio)) throw new ReglaNegocioException("El fin de " + nombre + " debe ser posterior al inicio");
    }
    private CarreraRespuestaDto carrera(Carrera c) { return new CarreraRespuestaDto(c.getId(), c.getCodigo(), c.getNombre(), c.isActiva()); }
    private PlanRespuestaDto plan(PlanEstudio p) { return new PlanRespuestaDto(p.getId(), p.getVersion(), p.getVigenciaDesde(), p.getVigenciaHasta(), p.getCarrera().getId()); }
    private AsignaturaRespuestaDto asignatura(Asignatura a) { return new AsignaturaRespuestaDto(a.getId(), a.getCodigo(), a.getNombre(), a.getCuatrimestreSugerido(), a.getCargaHoraria(), a.getPlan().getId()); }
    private CorrelatividadRespuestaDto correlatividad(Correlatividad c) { return new CorrelatividadRespuestaDto(c.getId(), c.getAsignatura().getId(), c.getCorrelativa().getId(), c.getTipo()); }
}

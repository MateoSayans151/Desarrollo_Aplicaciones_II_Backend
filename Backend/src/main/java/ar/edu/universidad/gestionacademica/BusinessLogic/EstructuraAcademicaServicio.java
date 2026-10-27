package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
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
        Carrera entidad = Carrera.builder().codigo(dto.codigo()).nombre(dto.nombre())
                .facultad(dto.facultad()).titulo(dto.titulo()).build();
        return carrera(carreras.save(entidad));
    }

    @Transactional(readOnly = true)
    public List<CarreraRespuestaDto> listarCarreras() {
        return carreras.findAll().stream().map(this::carrera).toList();
    }

    public CarreraRespuestaDto actualizarCarrera(Long id, CrearCarreraDto dto) {
        Carrera entidad = buscarCarrera(id);
        entidad.setCodigo(dto.codigo());
        entidad.setNombre(dto.nombre());
        entidad.setFacultad(dto.facultad());
        entidad.setTitulo(dto.titulo());
        return carrera(entidad);
    }

    public void eliminarCarrera(Long id) {
        Carrera entidad = buscarCarrera(id);
        entidad.setEstado(EstadoAcademico.INACTIVA);
    }

    public PlanRespuestaDto crearPlan(Long carreraId, CrearPlanDto dto) {
        PlanEstudio plan = PlanEstudio.builder().codigo(dto.codigo()).nombre(dto.nombre())
                .vigenciaDesde(dto.vigenciaDesde()).cantidadAsignaturas(dto.cantidadAsignaturas())
                .carrera(buscarCarrera(carreraId)).build();
        return plan(planes.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanRespuestaDto> listarPlanes(Long carreraId) {
        buscarCarrera(carreraId);
        return planes.findByCarreraIdOrderByVigenciaDesdeDesc(carreraId).stream().map(this::plan).toList();
    }

    public AsignaturaRespuestaDto crearAsignatura(Long planId, CrearAsignaturaDto dto) {
        Asignatura asignatura = Asignatura.builder().codigo(dto.codigo()).nombre(dto.nombre())
                .anio(dto.anio()).creditos(dto.creditos()).cargaHoraria(dto.cargaHoraria())
                .plan(buscarPlan(planId)).build();
        return asignatura(asignaturas.save(asignatura));
    }

    @Transactional(readOnly = true)
    public List<AsignaturaRespuestaDto> listarAsignaturas(Long planId) {
        buscarPlan(planId);
        return asignaturas.findByPlanIdOrderByNombreAsc(planId)
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
        if (correlatividades.existsByAsignaturaIdAndCorrelativaId(asignaturaId, dto.correlativaId())) {
            throw new ReglaNegocioException("La materia ya está configurada como correlativa para esta asignatura");
        }
        try {
            Correlatividad entidad = correlatividades.saveAndFlush(Correlatividad.builder().asignatura(destino)
                    .correlativa(requerida).tipo(dto.tipo()).build());
            return correlatividad(entidad);
        } catch (DataIntegrityViolationException ex) {
            throw new ReglaNegocioException("La materia ya está configurada como correlativa para esta asignatura");
        }
    }

    @Transactional(readOnly = true)
    public CondicionesRegularidadRespuestaDto listarCorrelatividades(Long asignaturaId) {
        Asignatura asignatura = buscarAsignatura(asignaturaId);
        List<Correlatividad> requisitos = correlatividades.findByAsignaturaId(asignaturaId);
        List<CondicionCorrelatividadDto> condiciones = Arrays.stream(Correlatividad.Tipo.values())
                .map(tipo -> new CondicionCorrelatividadDto(tipo, requisitos.stream()
                        .filter(requisito -> requisito.getTipo() == tipo)
                        .map(Correlatividad::getCorrelativa)
                        .map(correlativa -> new MateriaCorrelativaDto(correlativa.getId(),
                                correlativa.getCodigo(), correlativa.getNombre()))
                        .sorted(Comparator.comparing(MateriaCorrelativaDto::id))
                        .toList()))
                .filter(condicion -> !condicion.materiasCorrelativas().isEmpty())
                .toList();
        return new CondicionesRegularidadRespuestaDto(asignatura.getId(), condiciones);
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
    private CarreraRespuestaDto carrera(Carrera c) {
        return new CarreraRespuestaDto(c.getId(), c.getCodigo(), c.getNombre(), c.getFacultad(),
                calcularDuracion(asignaturas.countByPlan_Carrera_Id(c.getId())), c.getTitulo(), c.getEstado());
    }
    private String calcularDuracion(long cantidadAsignaturas) {
        long semestres = (cantidadAsignaturas + 4) / 5;
        if (semestres == 0) return "0 años";
        if (semestres % 2 == 0) {
            long anios = semestres / 2;
            return anios + (anios == 1 ? " año" : " años");
        }
        long aniosCompletos = semestres / 2;
        return aniosCompletos + ",5 años";
    }
    private PlanRespuestaDto plan(PlanEstudio p) {
        return new PlanRespuestaDto(p.getId(), p.getCodigo(), p.getNombre(), p.getVigenciaDesde(),
                p.getCantidadAsignaturas(), p.getEstado(), p.getCarrera().getId());
    }
    private AsignaturaRespuestaDto asignatura(Asignatura a) {
        return new AsignaturaRespuestaDto(a.getId(), a.getCodigo(), a.getNombre(), a.getAnio(),
                a.getCreditos(), a.getCargaHoraria(), a.getEstado(), a.getPlan().getId());
    }
    private CorrelatividadRespuestaDto correlatividad(Correlatividad c) { return new CorrelatividadRespuestaDto(c.getId(), c.getAsignatura().getId(), c.getCorrelativa().getId(), c.getTipo()); }
}

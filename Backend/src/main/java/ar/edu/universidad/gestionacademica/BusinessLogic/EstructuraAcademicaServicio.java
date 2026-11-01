package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EstructuraAcademicaServicio {
    private final CarreraRepositorio carreras;
    private final PlanEstudioRepositorio planes;
    private final AsignaturaRepositorio asignaturas;
    private final CorrelatividadRepositorio correlatividades;
    private final CursoRepositorio cursos;
    private final AsignacionAulaRepositorio asignaciones;

    public CarreraRespuestaDto crearCarrera(CrearCarreraDto dto) {
        if (carreras.existsByCodigoIgnoreCase(dto.codigo())) {
            throw new ReglaNegocioException("Ya existe una carrera con el codigo indicado");
        }
        Carrera entidad = Carrera.builder().codigo(dto.codigo()).nombre(dto.nombre().trim())
                .facultad(dto.facultad().trim()).titulo(dto.titulo().trim()).build();
        return carrera(carreras.save(entidad));
    }

    @Transactional(readOnly = true)
    public List<CarreraRespuestaDto> listarCarreras() {
        return carreras.findAll().stream().sorted(Comparator.comparing(Carrera::getCodigo))
                .map(this::carrera).toList();
    }

    public CarreraRespuestaDto actualizarCarrera(Long id, CrearCarreraDto dto) {
        Carrera entidad = buscarCarrera(id);
        if (carreras.existsByCodigoIgnoreCaseAndIdNot(dto.codigo(), id)) {
            throw new ReglaNegocioException("Ya existe una carrera con el codigo indicado");
        }
        entidad.setCodigo(dto.codigo());
        entidad.setNombre(dto.nombre().trim());
        entidad.setFacultad(dto.facultad().trim());
        entidad.setTitulo(dto.titulo().trim());
        return carrera(entidad);
    }

    public void eliminarCarrera(Long id) {
        Carrera entidad = buscarCarrera(id);
        if (cursos.existsByAsignaturaPlanCarreraIdAndEstadoIn(id,
                List.of(EstadoCurso.PLANIFICADO, EstadoCurso.EN_CURSO))) {
            throw new ReglaNegocioException("No se puede desactivar una carrera con cursos planificados o en curso");
        }
        entidad.setEstado(EstadoAcademico.INACTIVA);
    }

    public PlanRespuestaDto crearPlan(Long carreraId, CrearPlanDto dto) {
        Carrera carrera = buscarCarrera(carreraId);
        validarCarreraActiva(carrera);
        validarVigencia(dto.vigenciaDesde());
        if (planes.existsByCarreraIdAndCodigo(carreraId, dto.codigo())) {
            throw new ReglaNegocioException("Ya existe un plan con ese código en la carrera");
        }
        PlanEstudio plan = PlanEstudio.builder().codigo(dto.codigo()).nombre(dto.nombre().trim())
                .vigenciaDesde(dto.vigenciaDesde()).cantidadAsignaturas(0)
                .estado(EstadoPlanEstudio.PLANIFICADO)
                .carrera(carrera).build();
        return plan(planes.save(plan));
    }

    @Transactional(readOnly = true)
    public List<PlanRespuestaDto> listarPlanes(Long carreraId) {
        buscarCarrera(carreraId);
        return planes.findByCarreraIdOrderByVigenciaDesdeDesc(carreraId).stream().map(this::plan).toList();
    }

    public PlanRespuestaDto actualizarPlan(Long planId, ActualizarPlanDto dto) {
        PlanEstudio entidad = buscarPlan(planId);
        if (entidad.getEstado() == EstadoPlanEstudio.ACTIVO && dto.estado() != EstadoPlanEstudio.ACTIVO) {
            throw new ReglaNegocioException("Un plan activo no puede cambiar de estado");
        }
        validarVigencia(dto.vigenciaDesde());
        if (dto.estado() == EstadoPlanEstudio.ACTIVO) validarActivacion(entidad, dto);
        if (planes.existsByCarreraIdAndCodigoAndIdNot(entidad.getCarrera().getId(), dto.codigo(), planId)) {
            throw new ReglaNegocioException("Ya existe un plan con ese código en la carrera");
        }
        entidad.setCodigo(dto.codigo());
        entidad.setNombre(dto.nombre().trim());
        entidad.setVigenciaDesde(dto.vigenciaDesde());
        entidad.setEstado(dto.estado());
        return plan(entidad);
    }

    public void eliminarPlan(Long planId) {
        PlanEstudio entidad = buscarPlan(planId);
        if (entidad.getEstado() != EstadoPlanEstudio.PLANIFICADO) {
            throw new ReglaNegocioException("Solo se pueden eliminar planes en estado PLANIFICADO");
        }
        if (cursos.existsByAsignaturaPlanId(planId) || asignaciones.existsByAsignaturaPlanId(planId)) {
            throw new ReglaNegocioException("No se puede eliminar un plan con cursos o asignaciones de aula");
        }
        Carrera carrera = entidad.getCarrera();
        correlatividades.deleteByPlanId(planId);
        asignaturas.deleteByPlanId(planId);
        planes.delete(entidad);
        planes.flush();
        actualizarDuracionCarrera(carrera);
    }

    public AsignaturaRespuestaDto crearAsignatura(Long planId, CrearAsignaturaDto dto) {
        PlanEstudio plan = buscarPlan(planId);
        validarCarreraActiva(plan.getCarrera());
        if (asignaturas.existsByPlanIdAndCodigo(planId, dto.codigo())) {
            throw new ReglaNegocioException("Ya existe una asignatura con ese código en el plan");
        }
        Asignatura asignatura = Asignatura.builder().codigo(dto.codigo()).nombre(dto.nombre().trim())
                .anio(dto.anio()).creditos(dto.creditos()).cargaHoraria(dto.cargaHoraria())
                .plan(plan).build();
        Asignatura guardada = asignaturas.saveAndFlush(asignatura);
        plan.setCantidadAsignaturas(Math.toIntExact(asignaturas.countByPlanId(planId)));
        actualizarDuracionCarrera(plan.getCarrera());
        return asignatura(guardada);
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
        if (requerida.getAnio() > destino.getAnio()) {
            throw new ReglaNegocioException("Una correlativa no puede ser de un año posterior al de la asignatura");
        }
        if (generaCiclo(asignaturaId, dto.correlativaId())) {
            throw new ReglaNegocioException("La correlatividad generaría un ciclo: "
                    + requerida.getCodigo() + " ya depende de " + destino.getCodigo());
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

    /** Busca si la materia requerida ya depende, directa o indirectamente, de la asignatura. */
    private boolean generaCiclo(Long asignaturaId, Long requeridaId) {
        Deque<Long> pendientes = new ArrayDeque<>(List.of(requeridaId));
        Set<Long> visitadas = new HashSet<>();
        while (!pendientes.isEmpty()) {
            Long actual = pendientes.pop();
            if (actual.equals(asignaturaId)) return true;
            if (!visitadas.add(actual)) continue;
            correlatividades.findByAsignaturaId(actual)
                    .forEach(requisito -> pendientes.push(requisito.getCorrelativa().getId()));
        }
        return false;
    }
    private void validarCarreraActiva(Carrera carrera) {
        if (carrera.getEstado() == EstadoAcademico.INACTIVA) {
            throw new ReglaNegocioException("La carrera está inactiva");
        }
    }
    private void validarVigencia(LocalDate vigenciaDesde) {
        if (vigenciaDesde.getYear() < 2000 || vigenciaDesde.getYear() > 2100) {
            throw new ReglaNegocioException("La vigencia del plan debe estar entre los años 2000 y 2100");
        }
    }
    private void validarActivacion(PlanEstudio plan, ActualizarPlanDto dto) {
        if (dto.vigenciaDesde().getYear() > LocalDate.now().getYear()) {
            throw new ReglaNegocioException("Un plan con vigencia en un año futuro debe quedar PLANIFICADO");
        }
        if (asignaturas.countByPlanId(plan.getId()) == 0) {
            throw new ReglaNegocioException("Un plan sin asignaturas no puede activarse");
        }
        validarCarreraActiva(plan.getCarrera());
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
        PlanEstudio ultimoPlanActivo = planes
                .findFirstByCarreraIdAndEstadoOrderByVigenciaDesdeDescIdDesc(c.getId(), EstadoPlanEstudio.ACTIVO)
                .orElse(null);
        return new CarreraRespuestaDto(c.getId(), c.getCodigo(), c.getNombre(), c.getFacultad(),
                c.getDuracion(), c.getTitulo(), c.getEstado(),
                ultimoPlanActivo == null ? null : plan(ultimoPlanActivo));
    }
    private void actualizarDuracionCarrera(Carrera carrera) {
        int maximaCantidad = planes.findByCarreraIdOrderByIdDesc(carrera.getId()).stream()
                .mapToInt(PlanEstudio::getCantidadAsignaturas)
                .max()
                .orElse(0);
        carrera.setDuracion(calcularDuracion(maximaCantidad));
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

package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.PlanificacionDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanificacionServicio {
    private final SedeRepositorio sedes;
    private final AulaRepositorio aulas;
    private final AsignaturaRepositorio asignaturas;
    private final AsignacionAulaRepositorio asignaciones;
    private final PeriodoAcademicoRepositorio periodos;
    private final TurnoExamenRepositorio turnos;

    public SedeRespuestaDto crearSede(CrearSedeDto dto) {
        Sede s = sedes.save(Sede.builder().nombre(dto.nombre()).direccion(dto.direccion()).build());
        return new SedeRespuestaDto(s.getId(), s.getNombre(), s.getDireccion());
    }
    @Transactional(readOnly = true)
    public List<SedeRespuestaDto> listarSedes() {
        return sedes.findAll().stream().map(s -> new SedeRespuestaDto(s.getId(), s.getNombre(), s.getDireccion())).toList();
    }
    public AulaRespuestaDto crearAula(Long sedeId, CrearAulaDto dto) {
        Sede sede = sedes.findById(sedeId).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        Aula a = aulas.save(Aula.builder().codigo(dto.codigo()).capacidadMaxima(dto.capacidadMaxima()).sede(sede).build());
        return aula(a);
    }
    @Transactional(readOnly = true)
    public List<AulaRespuestaDto> listarAulas(Long sedeId) {
        return aulas.findBySedeIdOrderByCodigo(sedeId).stream().map(this::aula).toList();
    }
    public AsignacionRespuestaDto asignarAula(CrearAsignacionDto dto) {
        if (!dto.horaFin().isAfter(dto.horaInicio())) throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        Aula aula = aulas.findById(dto.aulaId()).orElseThrow(() -> new RecursoNoEncontradoException("Aula no encontrada"));
        Asignatura asignatura = asignaturas.findById(dto.asignaturaId()).orElseThrow(() -> new RecursoNoEncontradoException("Asignatura no encontrada"));
        if (dto.cantidadEstudiantes() > aula.getCapacidadMaxima()) throw new ReglaNegocioException("La cantidad de estudiantes supera la capacidad del aula");
        if (asignaciones.existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(dto.aulaId(), dto.fecha(), dto.horaFin(), dto.horaInicio())) {
            throw new ReglaNegocioException("El aula ya esta ocupada en ese horario");
        }
        AsignacionAula a = asignaciones.save(AsignacionAula.builder().aula(aula).asignatura(asignatura)
                .fecha(dto.fecha()).horaInicio(dto.horaInicio()).horaFin(dto.horaFin())
                .cantidadEstudiantes(dto.cantidadEstudiantes()).build());
        return asignacion(a);
    }
    @Transactional(readOnly = true)
    public List<AsignacionRespuestaDto> agendaAula(Long aulaId, LocalDate fecha) {
        return asignaciones.findByAulaIdAndFechaOrderByHoraInicio(aulaId, fecha).stream().map(this::asignacion).toList();
    }
    public PeriodoRespuestaDto crearPeriodo(CrearPeriodoDto dto) {
        validarRango(dto.fechaInicio(), dto.fechaFin(), "cuatrimestre");
        PeriodoAcademico p = periodos.save(PeriodoAcademico.builder().anio(dto.anio()).numero(dto.numero())
                .fechaInicio(dto.fechaInicio()).fechaFin(dto.fechaFin()).build());
        return new PeriodoRespuestaDto(p.getId(), p.getAnio(), p.getNumero(), p.getFechaInicio(), p.getFechaFin());
    }
    @Transactional(readOnly = true)
    public List<PeriodoRespuestaDto> listarPeriodos() {
        return periodos.findAll().stream().map(p -> new PeriodoRespuestaDto(p.getId(), p.getAnio(), p.getNumero(), p.getFechaInicio(), p.getFechaFin())).toList();
    }
    public TurnoRespuestaDto crearTurno(CrearTurnoDto dto) {
        validarRango(dto.fechaInicio(), dto.fechaFin(), "turno de examen");
        validarRango(dto.inscripcionDesde(), dto.inscripcionHasta(), "inscripcion");
        if (dto.inscripcionHasta().isAfter(dto.fechaInicio())) throw new ReglaNegocioException("La inscripcion debe cerrar antes del inicio del turno");
        TurnoExamen t = turnos.save(TurnoExamen.builder().nombre(dto.nombre()).fechaInicio(dto.fechaInicio())
                .fechaFin(dto.fechaFin()).inscripcionDesde(dto.inscripcionDesde()).inscripcionHasta(dto.inscripcionHasta()).build());
        return turno(t);
    }
    @Transactional(readOnly = true)
    public List<TurnoRespuestaDto> listarTurnos() { return turnos.findAll().stream().map(this::turno).toList(); }

    private void validarRango(LocalDate inicio, LocalDate fin, String nombre) {
        if (!fin.isAfter(inicio)) throw new ReglaNegocioException("El fin de " + nombre + " debe ser posterior al inicio");
    }
    private AulaRespuestaDto aula(Aula a) { return new AulaRespuestaDto(a.getId(), a.getCodigo(), a.getCapacidadMaxima(), a.getSede().getId()); }
    private AsignacionRespuestaDto asignacion(AsignacionAula a) { return new AsignacionRespuestaDto(a.getId(), a.getAula().getId(), a.getAsignatura().getId(), a.getFecha(), a.getHoraInicio(), a.getHoraFin(), a.getCantidadEstudiantes()); }
    private TurnoRespuestaDto turno(TurnoExamen t) { return new TurnoRespuestaDto(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(), t.getInscripcionDesde(), t.getInscripcionHasta()); }
}

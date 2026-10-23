package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final CursoRepositorio cursos;
    private final PeriodoAcademicoRepositorio periodos;
    private final TurnoExamenRepositorio turnos;

    public SedeRespuestaDto crearSede(CrearSedeDto dto) {
        Sede s = sedes.save(Sede.builder().nombre(dto.nombre().trim()).direccion(dto.direccion().trim()).build());
        return sede(s);
    }
    @Transactional(readOnly = true)
    public List<SedeRespuestaDto> listarSedes() {
        return sedes.findAll().stream().map(this::sede).toList();
    }
    public AulaRespuestaDto crearAula(Long sedeId, CrearAulaDto dto) {
        Sede sede = sedes.findById(sedeId).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
        String codigo = dto.codigo().trim();
        String nombre = dto.nombre() == null || dto.nombre().isBlank() ? codigo : dto.nombre().trim();
        Aula a = aulas.save(Aula.builder().codigo(codigo).nombre(nombre)
                .capacidadMaxima(dto.capacidadMaxima())
                .tipo(TipoUbicacion.AULA).sede(sede).build());
        return aula(a);
    }
    @Transactional(readOnly = true)
    public List<AulaRespuestaDto> listarAulas(Long sedeId) {
        return aulas.findBySedeIdOrderByCodigo(sedeId).stream().map(this::aula).toList();
    }
    public AsignacionRespuestaDto asignarAula(CrearAsignacionDto dto) {
        if (!dto.horaFin().isAfter(dto.horaInicio())) throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        Aula aula = aulas.findById(dto.aulaId()).orElseThrow(() -> new RecursoNoEncontradoException("Aula no encontrada"));
        if (aula.getTipo() != TipoUbicacion.AULA) {
            throw new ReglaNegocioException("Solo se pueden asignar ubicaciones de tipo AULA a una asignatura");
        }
        Asignatura asignatura = asignaturas.findById(dto.asignaturaId()).orElseThrow(() -> new RecursoNoEncontradoException("Asignatura no encontrada"));
        if (dto.cantidadEstudiantes() > aula.getCapacidadMaxima()) throw new ReglaNegocioException("La cantidad de estudiantes supera la capacidad del aula");
        if (asignaciones.existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(dto.aulaId(), dto.fecha(), dto.horaFin(), dto.horaInicio())) {
            throw new ReglaNegocioException("El aula ya esta ocupada en ese horario");
        }
        if (cursos.countOcupacionesEnFecha(dto.aulaId(), dto.fecha().getDayOfWeek(), dto.fecha(),
                dto.horaInicio(), dto.horaFin(), EstadoCurso.CANCELADO) > 0) {
            throw new ReglaNegocioException("El aula ya esta ocupada por un curso en ese horario");
        }
        AsignacionAula a = asignaciones.save(AsignacionAula.builder().aula(aula).asignatura(asignatura)
                .fecha(dto.fecha()).horaInicio(dto.horaInicio()).horaFin(dto.horaFin())
                .cantidadEstudiantes(dto.cantidadEstudiantes()).build());
        return asignacion(a);
    }
    @Transactional(readOnly = true)
    public List<OcupacionAulaDto> agendaAula(Long aulaId, LocalDate fecha) {
        return agendaAula(aulaId, fecha, fecha);
    }
    @Transactional(readOnly = true)
    public List<OcupacionAulaDto> agendaAula(Long aulaId, LocalDate desde, LocalDate hasta) {
        validarAulaExistente(aulaId);
        validarRangoAgenda(desde, hasta);
        List<OcupacionAulaDto> ocupaciones = new ArrayList<>();
        asignaciones.findByAulaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(aulaId, desde, hasta)
                .stream().map(this::ocupacion).forEach(ocupaciones::add);
        cursos.findOcupacionesEnRango(aulaId, desde, hasta, EstadoCurso.CANCELADO)
                .forEach(curso -> agregarOcurrenciasCurso(ocupaciones, curso, desde, hasta));
        ocupaciones.sort(Comparator.comparing(OcupacionAulaDto::fecha)
                .thenComparing(OcupacionAulaDto::horaInicio)
                .thenComparing(OcupacionAulaDto::tipo)
                .thenComparing(OcupacionAulaDto::referenciaId));
        return ocupaciones;
    }
    @Transactional(readOnly = true)
    public List<AsignacionRespuestaDto> listarAsignaciones() {
        return asignaciones.findAllByOrderByFechaAscHoraInicioAsc().stream().map(this::asignacion).toList();
    }
    @Transactional(readOnly = true)
    public List<AsignacionRespuestaDto> agendaAsignatura(Long asignaturaId, LocalDate desde, LocalDate hasta) {
        if (!asignaturas.existsById(asignaturaId)) {
            throw new RecursoNoEncontradoException("Asignatura no encontrada");
        }
        validarRangoAgenda(desde, hasta);
        return asignaciones.findByAsignaturaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
                asignaturaId, desde, hasta).stream().map(this::asignacion).toList();
    }
    public PeriodoRespuestaDto crearPeriodo(CrearPeriodoDto dto) {
        validarRango(dto.fechaInicio(), dto.fechaFin(), "cuatrimestre");
        validarRango(dto.inscripcionDesde(), dto.inscripcionHasta(), "inscripcion");
        if (dto.inscripcionHasta().isAfter(dto.fechaInicio())) {
            throw new ReglaNegocioException("La inscripcion debe cerrar antes o el mismo dia del inicio del cuatrimestre");
        }
        PeriodoAcademico p = periodos.save(PeriodoAcademico.builder().anio(dto.anio()).numero(dto.numero())
                .fechaInicio(dto.fechaInicio()).fechaFin(dto.fechaFin())
                .inscripcionDesde(dto.inscripcionDesde()).inscripcionHasta(dto.inscripcionHasta()).build());
        return periodo(p);
    }
    @Transactional(readOnly = true)
    public List<PeriodoRespuestaDto> listarPeriodos() {
        return periodos.findAll().stream().map(this::periodo).toList();
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
    private void validarRangoAgenda(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha de fin debe ser igual o posterior a la de inicio");
        }
    }
    private void validarAulaExistente(Long aulaId) {
        if (!aulas.existsById(aulaId)) {
            throw new RecursoNoEncontradoException("Aula no encontrada");
        }
    }
    private SedeRespuestaDto sede(Sede s) {
        return new SedeRespuestaDto(s.getId(), s.getNombre(), s.getDireccion(), s.getBackofficeSiteId(),
                s.getEstado());
    }
    private AulaRespuestaDto aula(Aula a) {
        return new AulaRespuestaDto(a.getId(), a.getCodigo(), a.getNombre(), a.getBackofficeLocationId(), a.getTipo(),
                a.getCapacidadMaxima(), a.getSede().getId(), a.getEstado());
    }
    private AsignacionRespuestaDto asignacion(AsignacionAula a) { return new AsignacionRespuestaDto(a.getId(), a.getAula().getId(), a.getAsignatura().getId(), a.getFecha(), a.getHoraInicio(), a.getHoraFin(), a.getCantidadEstudiantes()); }
    private OcupacionAulaDto ocupacion(AsignacionAula a) {
        return new OcupacionAulaDto(TipoOcupacionAula.ASIGNACION, a.getId(), a.getAula().getId(),
                a.getAsignatura().getId(), a.getAsignatura().getCodigo(), a.getAsignatura().getNombre(),
                null, a.getFecha(), a.getHoraInicio(), a.getHoraFin(), a.getCantidadEstudiantes());
    }
    private void agregarOcurrenciasCurso(List<OcupacionAulaDto> ocupaciones, Curso curso,
                                          LocalDate desde, LocalDate hasta) {
        if (curso.getDiaSemana() == null || curso.getHoraInicio() == null || curso.getHoraFin() == null) return;
        LocalDate inicio = desde.isAfter(curso.getFechaInicio()) ? desde : curso.getFechaInicio();
        LocalDate fin = hasta.isBefore(curso.getFechaFin()) ? hasta : curso.getFechaFin();
        LocalDate fecha = inicio.with(TemporalAdjusters.nextOrSame(curso.getDiaSemana()));
        while (!fecha.isAfter(fin)) {
            ocupaciones.add(new OcupacionAulaDto(TipoOcupacionAula.CURSO, curso.getId(),
                    curso.getAula().getId(), curso.getAsignatura().getId(), curso.getAsignatura().getCodigo(),
                    curso.getAsignatura().getNombre(), curso.getCodigo(), fecha, curso.getHoraInicio(),
                    curso.getHoraFin(), null));
            fecha = fecha.plusWeeks(1);
        }
    }
    private PeriodoRespuestaDto periodo(PeriodoAcademico p) { return new PeriodoRespuestaDto(p.getId(), p.getAnio(), p.getNumero(), p.getFechaInicio(), p.getFechaFin(), p.getInscripcionDesde(), p.getInscripcionHasta()); }
    private TurnoRespuestaDto turno(TurnoExamen t) { return new TurnoRespuestaDto(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin(), t.getInscripcionDesde(), t.getInscripcionHasta()); }
}

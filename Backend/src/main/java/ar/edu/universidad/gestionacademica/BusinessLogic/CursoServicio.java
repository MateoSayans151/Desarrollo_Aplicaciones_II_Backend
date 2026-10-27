package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.*;
import ar.edu.universidad.gestionacademica.Excepciones.*;
import ar.edu.universidad.gestionacademica.Integracion.Analitica.EventoResultadoPublicado;
import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static ar.edu.universidad.gestionacademica.Modelos.CursoDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CursoServicio {
    private final CursoRepositorio cursos;
    private final AsignaturaRepositorio asignaturas;
    private final PeriodoAcademicoRepositorio periodos;
    private final SedeRepositorio sedes;
    private final AulaRepositorio aulas;
    private final AsignacionAulaRepositorio asignaciones;
    private final CursoDocenteRepositorio cursoDocentes;
    private final InscripcionCursoRepositorio inscripciones;
    private final CorrelatividadRepositorio correlatividades;
    private final ApplicationEventPublisher eventos;

    public CursoRespuestaDto crear(CrearCursoDto dto) {
        PeriodoAcademico periodo = periodos.findById(dto.periodoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
        validarFechas(dto.fechaInicio(), dto.fechaFin(), periodo);
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new ReglaNegocioException("La hora de fin del curso debe ser posterior a la de inicio");
        }
        Aula aula = validarAula(dto.aulaId(), dto.sedeId(), dto.cupoMaximo());
        validarDisponibilidad(aula.getId(), dto.diaSemana(), periodo.getId(), dto.horaInicio(), dto.horaFin());
        validarAsignacionesPuntuales(aula.getId(), dto.diaSemana(), dto.fechaInicio(), dto.fechaFin(),
                dto.horaInicio(), dto.horaFin());
        Curso curso = cursos.save(Curso.builder()
                .codigo(dto.codigo().trim()).asignatura(buscarAsignatura(dto.asignaturaId())).periodo(periodo)
                .sede(buscarSede(dto.sedeId())).modalidad(dto.modalidad()).cupoMaximo(dto.cupoMaximo())
                .fechaInicio(dto.fechaInicio()).fechaFin(dto.fechaFin())
                .turno(dto.turno()).horaInicio(dto.horaInicio()).horaFin(dto.horaFin())
                .diaSemana(dto.diaSemana()).aula(aula).build());
        return respuesta(curso);
    }

    @Transactional(readOnly = true)
    public List<CursoRespuestaDto> listar(Long periodoId) {
        List<Curso> encontrados = periodoId == null ? cursos.findAll() : cursos.findByPeriodoIdOrderByCodigo(periodoId);
        return encontrados.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public CursoDetalleAcademicoRespuestaDto detalle(Long cursoId) {
        Curso curso = buscarCurso(cursoId);
        return new CursoDetalleAcademicoRespuestaDto(respuestaAcademica(curso),
                cursoDocentes.findByCursoIdOrderById(cursoId).stream().map(this::respuesta).toList(),
                inscripciones.findByCursoIdOrderByAlumnoId(cursoId).stream().map(this::respuesta).toList());
    }

    @Transactional(readOnly = true)
    public List<CursoDocenteRespuestaDto> listarDocentes(Long cursoId) {
        buscarCurso(cursoId);
        return cursoDocentes.findByCursoIdOrderById(cursoId).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<InscripcionCursoRespuestaDto> listarAlumnos(Long cursoId) {
        buscarCurso(cursoId);
        return inscripciones.findByCursoIdOrderByAlumnoId(cursoId).stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public List<InscripcionAlumnoRespuestaDto> listarInscripcionesAlumno(String alumnoId, Long periodoId) {
        String alumnoIdNormalizado = alumnoId.trim();
        List<InscripcionCurso> encontradas = periodoId == null
                ? inscripciones.findByAlumnoIdOrderByCursoPeriodoIdDescCursoCodigoAsc(alumnoIdNormalizado)
                : inscripciones.findByAlumnoIdAndCursoPeriodoIdOrderByCursoCodigoAsc(alumnoIdNormalizado, periodoId);
        return encontradas.stream().map(this::respuestaAlumno).toList();
    }

    public CursoDocenteRespuestaDto asignarDocente(Long cursoId, CrearCursoDocenteDto dto) {
        Curso curso = buscarCurso(cursoId);
        if (cursoDocentes.existsByCursoIdAndDocenteId(cursoId, dto.docenteId())) {
            throw new ReglaNegocioException("El docente ya está asignado al curso");
        }
        CursoDocente asignacion = cursoDocentes.save(CursoDocente.builder().curso(curso)
                .docenteId(dto.docenteId().trim()).rol(dto.rol()).build());
        return respuesta(asignacion);
    }

    public CursoRespuestaDto actualizarEstado(Long cursoId, ActualizarEstadoCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        curso.setEstado(dto.estado());
        return respuesta(curso);
    }

    public InscripcionCursoRespuestaDto inscribirAlumno(Long cursoId, CrearInscripcionCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        if (curso.getEstado() == EstadoCurso.CANCELADO) {
            throw new ReglaNegocioException("No se permiten inscripciones en un curso cancelado");
        }
        validarVentanaInscripcion(curso.getPeriodo());
        if (inscripciones.findByCursoIdAndAlumnoId(cursoId, dto.alumnoId().trim()).isPresent()) {
            throw new ReglaNegocioException("El alumno ya se encuentra inscripto en el curso");
        }
        if (inscripciones.countByCursoIdAndEstadoNot(cursoId, EstadoInscripcionCurso.BAJA) >= curso.getCupoMaximo()) {
            throw new ReglaNegocioException("El curso alcanzó su cupo máximo");
        }
        validarCorrelatividades(dto.alumnoId().trim(), curso.getAsignatura().getId());
        InscripcionCurso inscripcion = inscripciones.save(InscripcionCurso.builder().curso(curso)
                .alumnoId(dto.alumnoId().trim()).fechaInscripcion(LocalDate.now()).build());
        return respuesta(inscripcion);
    }

    public InscripcionCursoRespuestaDto actualizarInscripcion(Long cursoId, String alumnoId,
                                                                ActualizarInscripcionCursoDto dto) {
        InscripcionCurso inscripcion = inscripciones.findByCursoIdAndAlumnoId(cursoId, alumnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
        validarResultadoFinal(dto);
        inscripcion.setEstado(dto.estado());
        inscripcion.setNotaFinal(dto.notaFinal());
        inscripcion.setFechaResultado(dto.fechaResultado());
        if (esResultadoPublicado(inscripcion)) {
            eventos.publishEvent(new EventoResultadoPublicado(eventoResultado(inscripcion)));
        }
        return respuesta(inscripcion);
    }

    @Transactional(readOnly = true)
    public List<NotaFinalCursoRespuestaDto> listarNotas(Long cursoId) {
        buscarCurso(cursoId);
        return inscripciones.findByCursoIdOrderByAlumnoId(cursoId).stream().map(this::respuestaNota).toList();
    }

    @Transactional(readOnly = true)
    public NotaFinalCursoRespuestaDto consultarNota(Long cursoId, String alumnoId) {
        return respuestaNota(buscarInscripcion(cursoId, alumnoId));
    }

    public NotaFinalCursoRespuestaDto cargarNota(Long cursoId, String alumnoId, CargarNotaFinalDto dto) {
        if (dto.estado() != EstadoInscripcionCurso.APROBADO
                && dto.estado() != EstadoInscripcionCurso.DESAPROBADO) {
            throw new ReglaNegocioException("La nota final requiere estado APROBADO o DESAPROBADO");
        }
        InscripcionCurso inscripcion = buscarInscripcion(cursoId, alumnoId);
        if (inscripcion.getEstado() == EstadoInscripcionCurso.BAJA) {
            throw new ReglaNegocioException("No se puede cargar una nota final a una inscripción dada de baja");
        }
        LocalDate fecha = dto.fechaResultado() == null ? LocalDate.now() : dto.fechaResultado();
        boolean cambio = inscripcion.getNotaFinal() == null
                || dto.notaFinal().compareTo(inscripcion.getNotaFinal()) != 0
                || dto.estado() != inscripcion.getEstado()
                || !fecha.equals(inscripcion.getFechaResultado());
        inscripcion.setNotaFinal(dto.notaFinal());
        inscripcion.setEstado(dto.estado());
        inscripcion.setFechaResultado(fecha);
        if (cambio) {
            eventos.publishEvent(new EventoResultadoPublicado(eventoResultado(inscripcion)));
        }
        return respuestaNota(inscripcion);
    }

    private InscripcionCurso buscarInscripcion(Long cursoId, String alumnoId) {
        return inscripciones.findByCursoIdAndAlumnoId(cursoId, alumnoId.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada"));
    }

    private NotaFinalCursoRespuestaDto respuestaNota(InscripcionCurso inscripcion) {
        return new NotaFinalCursoRespuestaDto(inscripcion.getAlumnoId(), inscripcion.getNotaFinal(),
                inscripcion.getEstado(), inscripcion.getFechaResultado());
    }

    public CursoRespuestaDto actualizarHorario(Long cursoId, ActualizarHorarioCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
        Aula aula = validarAula(dto.aulaId(), curso.getSede().getId(), curso.getCupoMaximo());
        if (cursos.existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThanAndIdNot(
                aula.getId(), dto.diaSemana(), curso.getPeriodo().getId(), dto.horaFin(), dto.horaInicio(), cursoId)) {
            throw new ReglaNegocioException("El aula ya está ocupada en ese horario durante el período del curso");
        }
        validarAsignacionesPuntuales(aula.getId(), dto.diaSemana(), curso.getFechaInicio(), curso.getFechaFin(),
                dto.horaInicio(), dto.horaFin());
        curso.setAula(aula);
        curso.setDiaSemana(dto.diaSemana());
        curso.setHoraInicio(dto.horaInicio());
        curso.setHoraFin(dto.horaFin());
        return respuesta(curso);
    }

    private Aula validarAula(Long aulaId, Long sedeId, int cupoMaximo) {
        Aula aula = aulas.findById(aulaId).orElseThrow(() -> new RecursoNoEncontradoException("Aula no encontrada"));
        if (aula.getTipo() != TipoUbicacion.AULA) {
            throw new ReglaNegocioException("Solo se pueden asignar ubicaciones de tipo AULA a un curso");
        }
        if (!aula.getSede().getId().equals(sedeId)) {
            throw new ReglaNegocioException("El aula debe pertenecer a la sede del curso");
        }
        if (cupoMaximo > aula.getCapacidadMaxima()) {
            throw new ReglaNegocioException("El cupo del curso supera la capacidad del aula");
        }
        return aula;
    }

    private void validarDisponibilidad(Long aulaId, java.time.DayOfWeek diaSemana, Long periodoId,
                                       java.time.LocalTime horaInicio, java.time.LocalTime horaFin) {
        if (cursos.existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThan(
                aulaId, diaSemana, periodoId, horaFin, horaInicio)) {
            throw new ReglaNegocioException("El aula ya está ocupada en ese horario durante el período del curso");
        }
    }

    private void validarAsignacionesPuntuales(Long aulaId, java.time.DayOfWeek diaSemana,
                                               LocalDate fechaInicio, LocalDate fechaFin,
                                               java.time.LocalTime horaInicio,
                                               java.time.LocalTime horaFin) {
        boolean existeConflicto = asignaciones.findConflictosConCurso(
                        aulaId, fechaInicio, fechaFin, horaInicio, horaFin).stream()
                .anyMatch(asignacion -> asignacion.getFecha().getDayOfWeek() == diaSemana);
        if (existeConflicto) {
            throw new ReglaNegocioException("El aula tiene una asignación puntual en ese horario durante el curso");
        }
    }

    private Curso buscarCurso(Long id) {
        return cursos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));
    }
    private Asignatura buscarAsignatura(Long id) {
        return asignaturas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Asignatura no encontrada"));
    }
    private Sede buscarSede(Long id) {
        return sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede no encontrada"));
    }
    private boolean esResultadoPublicado(InscripcionCurso inscripcion) {
        return inscripcion.getEstado() == EstadoInscripcionCurso.APROBADO
                || inscripcion.getEstado() == EstadoInscripcionCurso.DESAPROBADO;
    }
    private void validarResultadoFinal(ActualizarInscripcionCursoDto dto) {
        boolean estadoFinal = dto.estado() == EstadoInscripcionCurso.APROBADO
                || dto.estado() == EstadoInscripcionCurso.DESAPROBADO;
        if (estadoFinal && (dto.notaFinal() == null || dto.fechaResultado() == null)) {
            throw new ReglaNegocioException("Un resultado final requiere nota final y fecha de resultado");
        }
    }
    private EventoNotificacionDto eventoResultado(InscripcionCurso inscripcion) {
        Curso curso = inscripcion.getCurso();
        PeriodoAcademico periodo = curso.getPeriodo();
        String docente = cursoDocentes.findByCursoIdOrderById(curso.getId()).stream()
                .min(Comparator.comparingInt(asignacion -> prioridadDocente(asignacion.getRol())))
                .map(CursoDocente::getDocenteId)
                .orElse(null);
        EventoNotificacionDto.Payload payload = new EventoNotificacionDto.Payload(
                inscripcion.getAlumnoId(), curso.getId(), curso.getAsignatura().getCodigo(), curso.getCodigo(),
                docente, curso.getSede().getNombre(), periodo.getAnio() + "-" + periodo.getNumero() + "Q",
                inscripcion.getNotaFinal(), inscripcion.getEstado(),
                inscripcion.getEstado() == EstadoInscripcionCurso.APROBADO, inscripcion.getFechaResultado());
        return new EventoNotificacionDto(
                "acad-res-" + inscripcion.getAlumnoId() + "-curso-" + curso.getId(),
                "academica", "resultado.publicado", Instant.now(), payload);
    }
    private int prioridadDocente(RolDocenteCurso rol) {
        return switch (rol) {
            case TITULAR -> 0;
            case ADJUNTO -> 1;
            case AUXILIAR -> 2;
        };
    }
    private void validarFechas(LocalDate inicio, LocalDate fin, PeriodoAcademico periodo) {
        if (!fin.isAfter(inicio)) throw new ReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio");
        if (inicio.isBefore(periodo.getFechaInicio()) || fin.isAfter(periodo.getFechaFin())) {
            throw new ReglaNegocioException("Las fechas del curso deben estar comprendidas en su período académico");
        }
    }
    private void validarVentanaInscripcion(PeriodoAcademico periodo) {
        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(periodo.getInscripcionDesde()) || hoy.isAfter(periodo.getInscripcionHasta())) {
            throw new ReglaNegocioException("La inscripcion se encuentra fuera de la ventana habilitada para el cuatrimestre");
        }
    }
    private void validarCorrelatividades(String alumnoId, Long asignaturaId) {
        for (Correlatividad correlatividad : correlatividades.findByAsignaturaId(asignaturaId)) {
            Set<EstadoInscripcionCurso> estadosValidos = correlatividad.getTipo() == Correlatividad.Tipo.REGULAR
                    ? Set.of(EstadoInscripcionCurso.REGULAR, EstadoInscripcionCurso.APROBADO)
                    : Set.of(EstadoInscripcionCurso.APROBADO);
            if (!inscripciones.existsByAlumnoIdAndCursoAsignaturaIdAndEstadoIn(alumnoId,
                    correlatividad.getCorrelativa().getId(), estadosValidos)) {
                throw new ReglaNegocioException("No se cumple la correlatividad " + correlatividad.getTipo()
                        + " requerida para la asignatura");
            }
        }
    }
    private CursoRespuestaDto respuesta(Curso c) {
        return new CursoRespuestaDto(c.getId(), c.getCodigo(), c.getAsignatura().getId(), c.getPeriodo().getId(),
                c.getSede().getId(), c.getModalidad(), c.getEstado(), c.getCupoMaximo(), c.getFechaInicio(),
                c.getFechaFin(), c.getTurno(), c.getHoraInicio(), c.getHoraFin(), c.getDiaSemana(),
                c.getAula() == null ? null : c.getAula().getId(),
                inscripciones.countByCursoIdAndEstadoNot(c.getId(), EstadoInscripcionCurso.BAJA),
                c.getSede().getNombre(), c.getAula() == null ? null : c.getAula().getNombre());
    }
    private CursoAcademicoRespuestaDto respuestaAcademica(Curso c) {
        AsignaturaResumenDto asignatura = new AsignaturaResumenDto(c.getAsignatura().getId(),
                c.getAsignatura().getCodigo(), c.getAsignatura().getNombre());
        PeriodoAcademico p = c.getPeriodo();
        PeriodoResumenDto periodo = new PeriodoResumenDto(p.getId(), p.getAnio(), p.getNumero(),
                p.getFechaInicio(), p.getFechaFin(), p.getInscripcionDesde(), p.getInscripcionHasta());
        return new CursoAcademicoRespuestaDto(c.getId(), c.getCodigo(), asignatura, periodo,
                c.getSede().getId(), c.getModalidad(), c.getEstado(), c.getCupoMaximo(),
                c.getFechaInicio(), c.getFechaFin(), c.getTurno(), c.getHoraInicio(), c.getHoraFin(),
                c.getDiaSemana(), c.getAula() == null ? null : c.getAula().getId(),
                inscripciones.countByCursoIdAndEstadoNot(c.getId(), EstadoInscripcionCurso.BAJA),
                c.getSede().getNombre(), c.getAula() == null ? null : c.getAula().getNombre());
    }
    private CursoDocenteRespuestaDto respuesta(CursoDocente cd) {
        return new CursoDocenteRespuestaDto(cd.getId(), cd.getDocenteId(), cd.getRol());
    }
    private InscripcionCursoRespuestaDto respuesta(InscripcionCurso i) {
        return new InscripcionCursoRespuestaDto(i.getId(), i.getAlumnoId(), i.getFechaInscripcion(), i.getEstado(),
                i.getNotaFinal(), i.getFechaResultado());
    }
    private InscripcionAlumnoRespuestaDto respuestaAlumno(InscripcionCurso i) {
        return new InscripcionAlumnoRespuestaDto(i.getId(), i.getAlumnoId(), i.getFechaInscripcion(), i.getEstado(),
                i.getNotaFinal(), i.getFechaResultado(), respuestaAcademica(i.getCurso()));
    }
}

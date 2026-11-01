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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static ar.edu.universidad.gestionacademica.Modelos.CursoDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CursoServicio {
    private static final LocalTime HORA_APERTURA = LocalTime.of(7, 0);
    private static final LocalTime HORA_INICIO_TARDE = LocalTime.of(13, 0);
    private static final LocalTime HORA_INICIO_NOCHE = LocalTime.of(18, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(23, 0);

    private final CursoRepositorio cursos;
    private final AsignaturaRepositorio asignaturas;
    private final PeriodoAcademicoRepositorio periodos;
    private final SedeRepositorio sedes;
    private final AulaRepositorio aulas;
    private final AsignacionAulaRepositorio asignaciones;
    private final CursoDocenteRepositorio cursoDocentes;
    private final InscripcionCursoRepositorio inscripciones;
    private final CorrelatividadRepositorio correlatividades;
    private final RegularidadCursoRepositorio regularidades;
    private final ApplicationEventPublisher eventos;

    public CursoRespuestaDto crear(CrearCursoDto dto) {
        PeriodoAcademico periodo = periodos.findById(dto.periodoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
        if (periodo.getFechaFin().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("No se pueden crear cursos en un período académico finalizado");
        }
        validarFechas(dto.fechaInicio(), dto.fechaFin(), periodo);
        validarHorario(dto.turno(), dto.horaInicio(), dto.horaFin());
        validarOcurrencia(dto.diaSemana(), dto.fechaInicio(), dto.fechaFin());
        Aula aula = validarAula(dto.aulaId(), dto.sedeId(), dto.cupoMaximo());
        validarDisponibilidad(aula.getId(), dto.diaSemana(), periodo.getId(), dto.horaInicio(), dto.horaFin());
        validarAsignacionesPuntuales(aula.getId(), dto.diaSemana(), dto.fechaInicio(), dto.fechaFin(),
                dto.horaInicio(), dto.horaFin());
        String codigo = dto.codigo().trim();
        if (cursos.existsByPeriodoIdAndCodigo(periodo.getId(), codigo)) {
            throw new ReglaNegocioException("Ya existe un curso con ese código en el período");
        }
        Asignatura asignatura = buscarAsignatura(dto.asignaturaId());
        validarAsignaturaPlanificable(asignatura);
        Curso curso = cursos.save(Curso.builder()
                .codigo(codigo).asignatura(asignatura).periodo(periodo)
                .sede(buscarSede(dto.sedeId())).modalidad(dto.modalidad()).cupoMaximo(dto.cupoMaximo())
                .fechaInicio(dto.fechaInicio()).fechaFin(dto.fechaFin())
                .turno(dto.turno()).horaInicio(dto.horaInicio()).horaFin(dto.horaFin())
                .diaSemana(dto.diaSemana()).aula(aula).build());
        regularidades.save(RegularidadCurso.builder()
                .curso(curso).codigo(curso.getCodigo())
                .asistenciaMinima(dto.asistenciaMinima()).promedioMinimo(dto.promedioMinimo())
                .build());
        return respuesta(curso);
    }

    @Transactional(readOnly = true)
    public List<CursoRespuestaDto> listar(Long periodoId) {
        if (periodoId != null) {
            return cursos.findByPeriodoIdOrderByCodigo(periodoId).stream().map(this::respuesta).toList();
        }
        return cursos.findAll().stream()
                .sorted(Comparator.comparing(Curso::getFechaInicio).reversed().thenComparing(Curso::getCodigo))
                .map(this::respuesta).toList();
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
        validarCursoModificable(curso);
        String docenteId = dto.docenteId().trim();
        if (cursoDocentes.existsByCursoIdAndDocenteId(cursoId, docenteId)) {
            throw new ReglaNegocioException("El docente ya está asignado al curso");
        }
        CursoDocente asignacion = cursoDocentes.save(CursoDocente.builder().curso(curso)
                .docenteId(docenteId).rol(dto.rol()).build());
        return respuesta(asignacion);
    }

    public CursoRespuestaDto actualizarEstado(Long cursoId, ActualizarEstadoCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        validarTransicionEstado(curso, dto.estado());
        curso.setEstado(dto.estado());
        return respuesta(curso);
    }

    public InscripcionCursoRespuestaDto inscribirAlumno(Long cursoId, CrearInscripcionCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        if (curso.getEstado() == EstadoCurso.CANCELADO) {
            throw new ReglaNegocioException("No se permiten inscripciones en un curso cancelado");
        }
        if (curso.getEstado() == EstadoCurso.FINALIZADO) {
            throw new ReglaNegocioException("No se permiten inscripciones en un curso finalizado");
        }
        validarVentanaInscripcion(curso.getPeriodo());
        String alumnoId = dto.alumnoId().trim();
        Optional<InscripcionCurso> anterior = inscripciones.findByCursoIdAndAlumnoId(cursoId, alumnoId);
        if (anterior.isPresent() && anterior.get().getEstado() != EstadoInscripcionCurso.BAJA) {
            throw new ReglaNegocioException("El alumno ya se encuentra inscripto en el curso");
        }
        Long asignaturaId = curso.getAsignatura().getId();
        if (inscripciones.existsByAlumnoIdAndCursoAsignaturaIdAndEstadoIn(alumnoId, asignaturaId,
                Set.of(EstadoInscripcionCurso.APROBADO))) {
            throw new ReglaNegocioException("El alumno ya aprobó la asignatura");
        }
        if (inscripciones.existsByAlumnoIdAndCursoPeriodoIdAndCursoAsignaturaIdAndEstadoNotIn(alumnoId,
                curso.getPeriodo().getId(), asignaturaId,
                Set.of(EstadoInscripcionCurso.BAJA, EstadoInscripcionCurso.DESAPROBADO, EstadoInscripcionCurso.LIBRE))) {
            throw new ReglaNegocioException("El alumno ya cursa la asignatura en otra comisión del período");
        }
        if (inscripciones.countByCursoIdAndEstadoNot(cursoId, EstadoInscripcionCurso.BAJA) >= curso.getCupoMaximo()) {
            throw new ReglaNegocioException("El curso alcanzó su cupo máximo");
        }
        validarCorrelatividades(alumnoId, asignaturaId);
        if (anterior.isPresent()) {
            InscripcionCurso reinscripcion = anterior.get();
            reinscripcion.setEstado(EstadoInscripcionCurso.INSCRIPTO);
            reinscripcion.setFechaInscripcion(LocalDate.now());
            reinscripcion.setNotaFinal(null);
            reinscripcion.setFechaResultado(null);
            return respuesta(reinscripcion);
        }
        InscripcionCurso inscripcion = inscripciones.save(InscripcionCurso.builder().curso(curso)
                .alumnoId(alumnoId).fechaInscripcion(LocalDate.now()).build());
        return respuesta(inscripcion);
    }

    public InscripcionCursoRespuestaDto actualizarInscripcion(Long cursoId, String alumnoId,
                                                                ActualizarInscripcionCursoDto dto) {
        InscripcionCurso inscripcion = buscarInscripcion(cursoId, alumnoId);
        validarResultadoFinal(dto);
        validarEstadoInscripcion(inscripcion, dto);
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
        if (inscripcion.getCurso().getEstado() == EstadoCurso.CANCELADO) {
            throw new ReglaNegocioException("No se pueden cargar notas en un curso cancelado");
        }
        LocalDate fecha = dto.fechaResultado() == null ? LocalDate.now() : dto.fechaResultado();
        validarFechaResultado(fecha);
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
        validarCursoModificable(curso);
        validarHorario(curso.getTurno(), dto.horaInicio(), dto.horaFin());
        validarOcurrencia(dto.diaSemana(), curso.getFechaInicio(), curso.getFechaFin());
        Aula aula = validarAula(dto.aulaId(), curso.getSede().getId(), curso.getCupoMaximo());
        if (cursos.existsByAulaIdAndDiaSemanaAndPeriodoIdAndEstadoNotAndHoraInicioLessThanAndHoraFinGreaterThanAndIdNot(
                aula.getId(), dto.diaSemana(), curso.getPeriodo().getId(), EstadoCurso.CANCELADO,
                dto.horaFin(), dto.horaInicio(), cursoId)) {
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

    private void validarDisponibilidad(Long aulaId, DayOfWeek diaSemana, Long periodoId,
                                       LocalTime horaInicio, LocalTime horaFin) {
        if (cursos.existsByAulaIdAndDiaSemanaAndPeriodoIdAndEstadoNotAndHoraInicioLessThanAndHoraFinGreaterThan(
                aulaId, diaSemana, periodoId, EstadoCurso.CANCELADO, horaFin, horaInicio)) {
            throw new ReglaNegocioException("El aula ya está ocupada en ese horario durante el período del curso");
        }
    }

    private void validarAsignacionesPuntuales(Long aulaId, DayOfWeek diaSemana,
                                               LocalDate fechaInicio, LocalDate fechaFin,
                                               LocalTime horaInicio, LocalTime horaFin) {
        boolean existeConflicto = asignaciones.findConflictosConCurso(
                        aulaId, fechaInicio, fechaFin, horaInicio, horaFin).stream()
                .anyMatch(asignacion -> asignacion.getFecha().getDayOfWeek() == diaSemana);
        if (existeConflicto) {
            throw new ReglaNegocioException("El aula tiene una asignación puntual en ese horario durante el curso");
        }
    }

    private void validarHorario(TurnoCurso turno, LocalTime horaInicio, LocalTime horaFin) {
        if (!horaFin.isAfter(horaInicio)) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
        if (horaInicio.isBefore(HORA_APERTURA) || horaFin.isAfter(HORA_CIERRE)) {
            throw new ReglaNegocioException("El horario del curso debe estar entre las 07:00 y las 23:00");
        }
        if (turno == null) return;
        boolean coincide = switch (turno) {
            case MAÑANA -> horaInicio.isBefore(HORA_INICIO_TARDE);
            case TARDE -> !horaInicio.isBefore(HORA_INICIO_TARDE) && horaInicio.isBefore(HORA_INICIO_NOCHE);
            case NOCHE -> !horaInicio.isBefore(HORA_INICIO_NOCHE);
        };
        if (!coincide) {
            throw new ReglaNegocioException("La hora de inicio no corresponde al turno " + turno
                    + " (mañana: 07 a 13, tarde: 13 a 18, noche: desde las 18)");
        }
    }

    private void validarOcurrencia(DayOfWeek diaSemana, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio.with(TemporalAdjusters.nextOrSame(diaSemana)).isAfter(fechaFin)) {
            throw new ReglaNegocioException("El día de cursada no ocurre entre las fechas del curso");
        }
    }

    private void validarCursoModificable(Curso curso) {
        if (curso.getEstado() == EstadoCurso.FINALIZADO || curso.getEstado() == EstadoCurso.CANCELADO) {
            throw new ReglaNegocioException("Un curso finalizado o cancelado no se puede modificar");
        }
    }

    private void validarTransicionEstado(Curso curso, EstadoCurso nuevo) {
        EstadoCurso actual = curso.getEstado();
        if (actual == nuevo) return;
        boolean permitida = switch (actual) {
            case PLANIFICADO -> nuevo == EstadoCurso.EN_CURSO || nuevo == EstadoCurso.CANCELADO;
            case EN_CURSO -> nuevo == EstadoCurso.FINALIZADO || nuevo == EstadoCurso.CANCELADO;
            case FINALIZADO, CANCELADO -> false;
        };
        if (!permitida) {
            throw new ReglaNegocioException("Un curso no puede pasar de " + actual + " a " + nuevo);
        }
        if (nuevo == EstadoCurso.EN_CURSO && LocalDate.now().isBefore(curso.getFechaInicio())) {
            throw new ReglaNegocioException("Un curso no puede comenzar antes de su fecha de inicio");
        }
    }

    /** Un plan en borrador o una carrera inactiva todavía no pueden ocupar aulas ni abrir comisiones. */
    static void validarAsignaturaPlanificable(Asignatura asignatura) {
        PlanEstudio plan = asignatura.getPlan();
        if (plan.getEstado() != EstadoPlanEstudio.ACTIVO) {
            throw new ReglaNegocioException("La asignatura pertenece a un plan de estudio que todavía no está activo");
        }
        if (plan.getCarrera().getEstado() == EstadoAcademico.INACTIVA) {
            throw new ReglaNegocioException("La asignatura pertenece a una carrera inactiva");
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
        if (dto.fechaResultado() != null) validarFechaResultado(dto.fechaResultado());
    }
    private void validarEstadoInscripcion(InscripcionCurso inscripcion, ActualizarInscripcionCursoDto dto) {
        boolean tomaBaja = dto.estado() == EstadoInscripcionCurso.BAJA;
        if (tomaBaja && dto.notaFinal() != null) {
            throw new ReglaNegocioException("Una inscripción dada de baja no puede tener nota final");
        }
        if (inscripcion.getEstado() == EstadoInscripcionCurso.BAJA && !tomaBaja) {
            throw new ReglaNegocioException("La inscripción está dada de baja; el alumno debe volver a inscribirse");
        }
    }
    private void validarFechaResultado(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de resultado no puede ser futura");
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

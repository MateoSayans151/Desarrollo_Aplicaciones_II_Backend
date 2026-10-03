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
    private final CursoDocenteRepositorio cursoDocentes;
    private final InscripcionCursoRepositorio inscripciones;
    private final HorarioCursoRepositorio horarios;
    private final CorrelatividadRepositorio correlatividades;
    private final ApplicationEventPublisher eventos;

    public CursoRespuestaDto crear(CrearCursoDto dto) {
        PeriodoAcademico periodo = periodos.findById(dto.periodoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Periodo académico no encontrado"));
        validarFechas(dto.fechaInicio(), dto.fechaFin(), periodo);
        Curso curso = cursos.save(Curso.builder()
                .codigo(dto.codigo().trim()).asignatura(buscarAsignatura(dto.asignaturaId())).periodo(periodo)
                .sede(buscarSede(dto.sedeId())).modalidad(dto.modalidad()).cupoMaximo(dto.cupoMaximo())
                .fechaInicio(dto.fechaInicio()).fechaFin(dto.fechaFin()).build());
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
                inscripciones.findByCursoIdOrderByAlumnoId(cursoId).stream().map(this::respuesta).toList(),
                horarios.findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(cursoId).stream().map(this::respuesta).toList());
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

    public HorarioCursoRespuestaDto agregarHorario(Long cursoId, CrearHorarioCursoDto dto) {
        Curso curso = buscarCurso(cursoId);
        if (!dto.horaFin().isAfter(dto.horaInicio())) {
            throw new ReglaNegocioException("La hora de fin debe ser posterior a la de inicio");
        }
        Aula aula = aulas.findById(dto.aulaId()).orElseThrow(() -> new RecursoNoEncontradoException("Aula no encontrada"));
        if (aula.getTipo() != TipoUbicacion.AULA) {
            throw new ReglaNegocioException("Solo se pueden asignar ubicaciones de tipo AULA a un curso");
        }
        if (!aula.getSede().getId().equals(curso.getSede().getId())) {
            throw new ReglaNegocioException("El aula debe pertenecer a la sede del curso");
        }
        if (curso.getCupoMaximo() > aula.getCapacidadMaxima()) {
            throw new ReglaNegocioException("El cupo del curso supera la capacidad del aula");
        }
        if (horarios.existsByAulaIdAndDiaSemanaAndCursoPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThan(
                aula.getId(), dto.diaSemana(), curso.getPeriodo().getId(), dto.horaFin(), dto.horaInicio())) {
            throw new ReglaNegocioException("El aula ya está ocupada en ese horario durante el período del curso");
        }
        HorarioCurso horario = horarios.save(HorarioCurso.builder().curso(curso).aula(aula)
                .diaSemana(dto.diaSemana()).horaInicio(dto.horaInicio()).horaFin(dto.horaFin()).build());
        return respuesta(horario);
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
                c.getFechaFin(), inscripciones.countByCursoIdAndEstadoNot(c.getId(), EstadoInscripcionCurso.BAJA));
    }
    private CursoAcademicoRespuestaDto respuestaAcademica(Curso c) {
        AsignaturaResumenDto asignatura = new AsignaturaResumenDto(c.getAsignatura().getId(),
                c.getAsignatura().getCodigo(), c.getAsignatura().getNombre());
        PeriodoAcademico p = c.getPeriodo();
        PeriodoResumenDto periodo = new PeriodoResumenDto(p.getId(), p.getAnio(), p.getNumero(),
                p.getFechaInicio(), p.getFechaFin(), p.getInscripcionDesde(), p.getInscripcionHasta());
        return new CursoAcademicoRespuestaDto(c.getId(), c.getCodigo(), asignatura, periodo,
                c.getSede().getId(), c.getModalidad(), c.getEstado(), c.getCupoMaximo(),
                c.getFechaInicio(), c.getFechaFin(),
                inscripciones.countByCursoIdAndEstadoNot(c.getId(), EstadoInscripcionCurso.BAJA));
    }
    private CursoDocenteRespuestaDto respuesta(CursoDocente cd) {
        return new CursoDocenteRespuestaDto(cd.getId(), cd.getDocenteId(), cd.getRol());
    }
    private InscripcionCursoRespuestaDto respuesta(InscripcionCurso i) {
        return new InscripcionCursoRespuestaDto(i.getId(), i.getAlumnoId(), i.getFechaInscripcion(), i.getEstado(),
                i.getNotaFinal(), i.getFechaResultado());
    }
    private HorarioCursoRespuestaDto respuesta(HorarioCurso h) {
        return new HorarioCursoRespuestaDto(h.getId(), h.getAula().getId(), h.getDiaSemana(), h.getHoraInicio(), h.getHoraFin());
    }
}

package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.EstadoInscripcionCurso;
import ar.edu.universidad.gestionacademica.Entidades.InscripcionCurso;
import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Repositorios.CorrelatividadRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.InscripcionCursoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Set;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegularidadServicio {
    private final CursoRepositorio cursos;
    private final InscripcionCursoRepositorio inscripciones;
    private final CorrelatividadRepositorio correlatividades;

    public ResultadoRegularidadDto validar(ValidarRegularidadDto datos) {
        String alumnoId = datos.alumnoId().trim();
        Curso curso = cursos.findById(datos.cursoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));
        InscripcionCurso inscripcion = inscripciones.findByCursoIdAndAlumnoId(curso.getId(), alumnoId)
                .orElseThrow(() -> new ReglaNegocioException("El alumno no está inscripto en el curso indicado"));
        if (inscripcion.getEstado() == EstadoInscripcionCurso.BAJA) {
            throw new ReglaNegocioException("La inscripción del alumno en el curso está dada de baja");
        }

        var motivos = new ArrayList<String>();
        if (datos.asistencia() < datos.asistenciaMinima()) motivos.add("No alcanza la asistencia mínima");
        if (datos.promedio() < datos.promedioMinimo()) motivos.add("No alcanza el promedio mínimo");
        boolean regular = motivos.isEmpty();

        for (Correlatividad correlatividad : correlatividades.findByAsignaturaId(curso.getAsignatura().getId())) {
            boolean aprobada = inscripciones.existsByAlumnoIdAndCursoAsignaturaIdAndEstadoIn(
                    alumnoId, correlatividad.getCorrelativa().getId(), Set.of(EstadoInscripcionCurso.APROBADO));
            if (!aprobada) {
                motivos.add("No tiene aprobada la correlativa " + correlatividad.getCorrelativa().getCodigo()
                        + " · " + correlatividad.getCorrelativa().getNombre());
            }
        }

        boolean habilitadoParaFinal = regular && motivos.isEmpty();
        return new ResultadoRegularidadDto(regular, habilitadoParaFinal, motivos, alumnoId,
                curso.getId(), curso.getCodigo(), curso.getAsignatura().getId(),
                curso.getAsignatura().getCodigo(), curso.getAsignatura().getNombre());
    }
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface InscripcionCursoRepositorio extends JpaRepository<InscripcionCurso, Long> {
    List<InscripcionCurso> findByCursoIdOrderByAlumnoId(Long cursoId);
    Optional<InscripcionCurso> findByCursoIdAndAlumnoId(Long cursoId, String alumnoId);
    @EntityGraph(attributePaths = {"curso", "curso.asignatura", "curso.periodo", "curso.sede", "curso.aula"})
    List<InscripcionCurso> findByAlumnoIdOrderByCursoPeriodoIdDescCursoCodigoAsc(String alumnoId);
    @EntityGraph(attributePaths = {"curso", "curso.asignatura", "curso.periodo", "curso.sede", "curso.aula"})
    List<InscripcionCurso> findByAlumnoIdAndCursoPeriodoIdOrderByCursoCodigoAsc(String alumnoId, Long periodoId);
    boolean existsByAlumnoIdAndCursoAsignaturaIdAndEstadoIn(String alumnoId, Long asignaturaId,
                                                            Collection<EstadoInscripcionCurso> estados);
    long countByCursoIdAndEstadoNot(Long cursoId, EstadoInscripcionCurso estado);
}

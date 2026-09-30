package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InscripcionCursoRepositorio extends JpaRepository<InscripcionCurso, Long> {
    List<InscripcionCurso> findByCursoIdOrderByAlumnoId(Long cursoId);
    Optional<InscripcionCurso> findByCursoIdAndAlumnoId(Long cursoId, String alumnoId);
    long countByCursoIdAndEstadoNot(Long cursoId, EstadoInscripcionCurso estado);
}

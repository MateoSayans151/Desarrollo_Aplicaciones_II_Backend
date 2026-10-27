package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.RegularidadCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegularidadCursoRepositorio extends JpaRepository<RegularidadCurso, Long> {
    Optional<RegularidadCurso> findByCursoId(Long cursoId);
    boolean existsByCursoId(Long cursoId);
}

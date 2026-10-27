package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrelatividadRepositorio extends JpaRepository<Correlatividad, Long> {
    @EntityGraph(attributePaths = {"correlativa"})
    List<Correlatividad> findByAsignaturaId(Long asignaturaId);
    boolean existsByAsignaturaIdAndCorrelativaId(Long asignaturaId, Long correlativaId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignaturaRepositorio extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findByPlanIdOrderByNombreAsc(Long planId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorrelatividadRepositorio extends JpaRepository<Correlatividad, Long> {
    List<Correlatividad> findByAsignaturaId(Long asignaturaId);
}

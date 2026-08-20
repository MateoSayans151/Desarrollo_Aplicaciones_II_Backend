package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarreraRepositorio extends JpaRepository<Carrera, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
}

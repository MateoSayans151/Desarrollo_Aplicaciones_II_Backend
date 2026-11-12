package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Sede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SedeRepositorio extends JpaRepository<Sede, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<Sede> findByNombre(String nombre);
    Optional<Sede> findByBackofficeSiteId(UUID backofficeSiteId);
    List<Sede> findByBackofficeSiteIdIsNotNull();
}

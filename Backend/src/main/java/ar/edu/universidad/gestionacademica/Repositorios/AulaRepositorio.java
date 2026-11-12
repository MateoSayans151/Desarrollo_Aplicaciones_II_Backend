package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AulaRepositorio extends JpaRepository<Aula, Long> {
    List<Aula> findBySedeIdOrderByCodigo(Long sedeId);
    boolean existsBySedeIdAndCodigoIgnoreCase(Long sedeId, String codigo);
    Optional<Aula> findBySedeIdAndCodigo(Long sedeId, String codigo);
    Optional<Aula> findByBackofficeLocationId(UUID backofficeLocationId);
    List<Aula> findBySedeIdAndBackofficeLocationIdIsNotNull(Long sedeId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AulaRepositorio extends JpaRepository<Aula, Long> {
    List<Aula> findBySedeIdOrderByCodigo(Long sedeId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepositorio extends JpaRepository<Curso, Long> {
    List<Curso> findByPeriodoIdOrderByCodigo(Long periodoId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.PlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEstudioRepositorio extends JpaRepository<PlanEstudio, Long> {
    List<PlanEstudio> findByCarreraIdOrderByVigenciaDesdeDesc(Long carreraId);
}

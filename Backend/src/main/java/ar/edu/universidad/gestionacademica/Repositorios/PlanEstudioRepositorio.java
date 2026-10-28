package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.PlanEstudio;
import ar.edu.universidad.gestionacademica.Entidades.EstadoPlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanEstudioRepositorio extends JpaRepository<PlanEstudio, Long> {
    List<PlanEstudio> findByCarreraIdOrderByVigenciaDesdeDesc(Long carreraId);
    List<PlanEstudio> findByCarreraIdOrderByIdDesc(Long carreraId);
    Optional<PlanEstudio> findFirstByCarreraIdAndEstadoOrderByIdDesc(Long carreraId, EstadoPlanEstudio estado);
}

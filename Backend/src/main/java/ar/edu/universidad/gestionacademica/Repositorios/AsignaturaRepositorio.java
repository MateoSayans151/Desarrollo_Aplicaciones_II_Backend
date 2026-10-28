package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AsignaturaRepositorio extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findByPlanIdOrderByNombreAsc(Long planId);
    long countByPlanId(Long planId);

    @Modifying
    @Query("delete from Asignatura a where a.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);
}

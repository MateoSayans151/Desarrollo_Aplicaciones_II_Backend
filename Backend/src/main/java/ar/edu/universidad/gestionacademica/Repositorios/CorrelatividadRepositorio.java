package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Correlatividad;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CorrelatividadRepositorio extends JpaRepository<Correlatividad, Long> {
    @EntityGraph(attributePaths = {"correlativa"})
    List<Correlatividad> findByAsignaturaId(Long asignaturaId);
    boolean existsByAsignaturaIdAndCorrelativaId(Long asignaturaId, Long correlativaId);

    @Modifying
    @Query("delete from Correlatividad c where c.asignatura.plan.id = :planId or c.correlativa.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);
}

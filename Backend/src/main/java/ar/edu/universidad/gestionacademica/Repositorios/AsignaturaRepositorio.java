package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AsignaturaRepositorio extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findByPlanIdOrderByNombreAsc(Long planId);
    @Query("select count(a) from Asignatura a where a.plan.carrera.id = :carreraId")
    long countByPlan_Carrera_Id(@Param("carreraId") Long carreraId);
}

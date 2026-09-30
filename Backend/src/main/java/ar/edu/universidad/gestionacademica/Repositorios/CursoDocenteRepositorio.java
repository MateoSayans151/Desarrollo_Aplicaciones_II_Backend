package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.CursoDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoDocenteRepositorio extends JpaRepository<CursoDocente, Long> {
    List<CursoDocente> findByCursoIdOrderById(Long cursoId);
    boolean existsByCursoIdAndDocenteId(Long cursoId, String docenteId);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.EventoPublicado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoPublicadoRepositorio extends JpaRepository<EventoPublicado, String> {
    List<EventoPublicado> findTop20ByEstadoOrderByCreadoEnAsc(String estado);
}

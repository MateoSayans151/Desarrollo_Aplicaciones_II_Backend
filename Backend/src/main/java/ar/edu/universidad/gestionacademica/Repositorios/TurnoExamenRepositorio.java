package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.TurnoExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface TurnoExamenRepositorio extends JpaRepository<TurnoExamen, Long> {
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(LocalDate fin, LocalDate inicio);
}

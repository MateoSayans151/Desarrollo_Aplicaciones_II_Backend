package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.TurnoExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TurnoExamenRepositorio extends JpaRepository<TurnoExamen, Long> {
    /** El turno que contiene la fecha (los turnos no se superponen). */
    Optional<TurnoExamen> findFirstByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(LocalDate fecha, LocalDate mismaFecha);
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(LocalDate fin, LocalDate inicio);
}

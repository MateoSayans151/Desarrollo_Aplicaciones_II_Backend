package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.AsignacionAula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.*;
import java.util.List;

public interface AsignacionAulaRepositorio extends JpaRepository<AsignacionAula, Long> {
    boolean existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long aulaId, LocalDate fecha, LocalTime horaFin, LocalTime horaInicio);

    List<AsignacionAula> findByAulaIdAndFechaOrderByHoraInicio(Long aulaId, LocalDate fecha);
}

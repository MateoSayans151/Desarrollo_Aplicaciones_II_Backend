package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalTime;

public interface CursoRepositorio extends JpaRepository<Curso, Long> {
    List<Curso> findByPeriodoIdOrderByCodigo(Long periodoId);
    boolean existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long aulaId, DayOfWeek diaSemana, Long periodoId, LocalTime horaFin, LocalTime horaInicio);
    boolean existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThanAndIdNot(
            Long aulaId, DayOfWeek diaSemana, Long periodoId, LocalTime horaFin, LocalTime horaInicio, Long id);
}

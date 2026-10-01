package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.HorarioCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface HorarioCursoRepositorio extends JpaRepository<HorarioCurso, Long> {
    List<HorarioCurso> findByCursoIdOrderByDiaSemanaAscHoraInicioAsc(Long cursoId);
    boolean existsByAulaIdAndDiaSemanaAndCursoPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long aulaId, DayOfWeek diaSemana, Long periodoId, LocalTime horaFin, LocalTime horaInicio);
}

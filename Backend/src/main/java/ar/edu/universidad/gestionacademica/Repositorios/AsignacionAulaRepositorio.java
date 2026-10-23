package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.AsignacionAula;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.*;
import java.util.List;

public interface AsignacionAulaRepositorio extends JpaRepository<AsignacionAula, Long> {
    boolean existsByAulaIdAndFechaAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long aulaId, LocalDate fecha, LocalTime horaFin, LocalTime horaInicio);

    @EntityGraph(attributePaths = {"aula", "asignatura"})
    List<AsignacionAula> findByAulaIdAndFechaOrderByHoraInicio(Long aulaId, LocalDate fecha);

    @EntityGraph(attributePaths = {"aula", "asignatura"})
    List<AsignacionAula> findByAulaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            Long aulaId, LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = {"aula", "asignatura"})
    List<AsignacionAula> findAllByOrderByFechaAscHoraInicioAsc();

    @EntityGraph(attributePaths = {"aula", "asignatura"})
    List<AsignacionAula> findByAsignaturaIdAndFechaBetweenOrderByFechaAscHoraInicioAsc(
            Long asignaturaId, LocalDate desde, LocalDate hasta);

    @Query("""
            select a from AsignacionAula a
            where a.aula.id = :aulaId
              and a.fecha between :desde and :hasta
              and a.horaInicio < :horaFin
              and a.horaFin > :horaInicio
            """)
    List<AsignacionAula> findConflictosConCurso(@Param("aulaId") Long aulaId,
                                                @Param("desde") LocalDate desde,
                                                @Param("hasta") LocalDate hasta,
                                                @Param("horaInicio") LocalTime horaInicio,
                                                @Param("horaFin") LocalTime horaFin);
}

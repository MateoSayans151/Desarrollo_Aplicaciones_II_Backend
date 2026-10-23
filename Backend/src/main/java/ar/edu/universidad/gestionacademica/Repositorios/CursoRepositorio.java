package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.EstadoCurso;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public interface CursoRepositorio extends JpaRepository<Curso, Long> {
    @Override
    @EntityGraph(attributePaths = {"asignatura", "periodo", "sede", "aula"})
    List<Curso> findAll();
    @EntityGraph(attributePaths = {"asignatura", "periodo", "sede", "aula"})
    List<Curso> findByPeriodoIdOrderByCodigo(Long periodoId);
    boolean existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThan(
            Long aulaId, DayOfWeek diaSemana, Long periodoId, LocalTime horaFin, LocalTime horaInicio);
    boolean existsByAulaIdAndDiaSemanaAndPeriodoIdAndHoraInicioLessThanAndHoraFinGreaterThanAndIdNot(
            Long aulaId, DayOfWeek diaSemana, Long periodoId, LocalTime horaFin, LocalTime horaInicio, Long id);

    @Query("""
            select count(c) from Curso c
            where c.aula.id = :aulaId
              and c.diaSemana = :diaSemana
              and c.fechaInicio <= :fecha
              and c.fechaFin >= :fecha
              and c.horaInicio < :horaFin
              and c.horaFin > :horaInicio
              and c.estado <> :estadoExcluido
            """)
    long countOcupacionesEnFecha(@Param("aulaId") Long aulaId,
                                 @Param("diaSemana") DayOfWeek diaSemana,
                                 @Param("fecha") LocalDate fecha,
                                 @Param("horaInicio") LocalTime horaInicio,
                                 @Param("horaFin") LocalTime horaFin,
                                 @Param("estadoExcluido") EstadoCurso estadoExcluido);

    @EntityGraph(attributePaths = {"asignatura", "aula"})
    @Query("""
            select c from Curso c
            where c.aula.id = :aulaId
              and c.fechaInicio <= :hasta
              and c.fechaFin >= :desde
              and c.estado <> :estadoExcluido
            order by c.fechaInicio, c.horaInicio
            """)
    List<Curso> findOcupacionesEnRango(@Param("aulaId") Long aulaId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta,
                                       @Param("estadoExcluido") EstadoCurso estadoExcluido);
}

package ar.edu.universidad.gestionacademica.Repositorios;

import ar.edu.universidad.gestionacademica.Entidades.PeriodoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PeriodoAcademicoRepositorio extends JpaRepository<PeriodoAcademico, Long> {
    boolean existsByAnioAndNumero(int anio, int numero);
    boolean existsByFechaInicioLessThanEqualAndFechaFinGreaterThanEqual(LocalDate fin, LocalDate inicio);
}

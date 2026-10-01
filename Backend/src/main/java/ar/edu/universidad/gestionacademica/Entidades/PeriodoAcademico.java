package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "periodos_academicos", uniqueConstraints = @UniqueConstraint(columnNames = {"anio", "numero"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PeriodoAcademico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Min(2000) @Column(nullable = false)
    private int anio;
    @Min(1) @Max(2) @Column(nullable = false)
    private int numero;
    @NotNull @Column(nullable = false)
    private LocalDate fechaInicio;
    @NotNull @Column(nullable = false)
    private LocalDate fechaFin;
    @NotNull @Column(nullable = false)
    private LocalDate inscripcionDesde;
    @NotNull @Column(nullable = false)
    private LocalDate inscripcionHasta;
}

package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "turnos_examen")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TurnoExamen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false)
    private String nombre;
    @NotNull @Column(nullable = false)
    private LocalDate fechaInicio;
    @NotNull @Column(nullable = false)
    private LocalDate fechaFin;
    @NotNull @Column(nullable = false)
    private LocalDate inscripcionDesde;
    @NotNull @Column(nullable = false)
    private LocalDate inscripcionHasta;
}

package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.*;

@Entity
@Table(name = "asignaciones_aula")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionAula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aula_id", nullable = false)
    private Aula aula;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;
    @NotNull @Column(nullable = false)
    private LocalDate fecha;
    @NotNull @Column(nullable = false)
    private LocalTime horaInicio;
    @NotNull @Column(nullable = false)
    private LocalTime horaFin;
    @Positive @Column(nullable = false)
    private int cantidadEstudiantes;
}

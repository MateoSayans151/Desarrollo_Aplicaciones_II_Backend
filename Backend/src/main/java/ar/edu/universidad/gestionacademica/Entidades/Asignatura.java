package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "asignaturas", uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asignatura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 20)
    private String codigo;
    @NotBlank @Column(nullable = false)
    private String nombre;
    @Min(1) @Max(10) @Column(nullable = false)
    private int cuatrimestreSugerido;
    @Positive @Column(nullable = false)
    private int cargaHoraria;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEstudio plan;
}

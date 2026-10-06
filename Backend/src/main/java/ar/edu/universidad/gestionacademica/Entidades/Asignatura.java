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
    @Min(1) @Column(nullable = false)
    private int anio;
    @Positive @Column(nullable = false)
    private int creditos;
    @Positive @Column(nullable = false)
    private int cargaHoraria;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private EstadoAcademico estado = EstadoAcademico.ACTIVA;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEstudio plan;
}

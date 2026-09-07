package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "planes_estudio", uniqueConstraints = @UniqueConstraint(columnNames = {"carrera_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanEstudio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 30)
    private String codigo;
    @NotBlank @Column(nullable = false)
    private String nombre;
    @NotNull @Column(nullable = false)
    private LocalDate vigenciaDesde;
    @PositiveOrZero @Column(nullable = false)
    private int cantidadAsignaturas;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private EstadoAcademico estado = EstadoAcademico.BORRADOR;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;
}

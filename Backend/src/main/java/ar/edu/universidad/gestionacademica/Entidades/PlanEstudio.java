package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "planes_estudio", uniqueConstraints = @UniqueConstraint(columnNames = {"carrera_id", "version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanEstudio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 30)
    private String version;
    @NotNull @Column(nullable = false)
    private LocalDate vigenciaDesde;
    private LocalDate vigenciaHasta;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;
}

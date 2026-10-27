package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "regularidades_curso", uniqueConstraints = {
        @UniqueConstraint(name = "uk_regularidad_curso", columnNames = "curso_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegularidadCurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false, foreignKey = @ForeignKey(name = "fk_regularidad_curso"))
    private Curso curso;

    @NotBlank
    @Column(nullable = false, length = 30)
    private String codigo;

    @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
    @Column(name = "asistencia_minima", nullable = false, precision = 5, scale = 2)
    private BigDecimal asistenciaMinima;

    @NotNull @DecimalMin("0.0") @DecimalMax("10.0")
    @Column(name = "promedio_minimo", nullable = false, precision = 4, scale = 2)
    private BigDecimal promedioMinimo;
}

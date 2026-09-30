package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "inscripciones_curso", uniqueConstraints = @UniqueConstraint(columnNames = {"curso_id", "alumno_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InscripcionCurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    // Identificador del módulo de alumnos/identidad; no se replica su información personal.
    @NotBlank @Column(name = "alumno_id", nullable = false, length = 100)
    private String alumnoId;

    @NotNull @Column(nullable = false)
    private LocalDate fechaInscripcion;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private EstadoInscripcionCurso estado = EstadoInscripcionCurso.INSCRIPTO;

    @DecimalMin("0.0") @DecimalMax("10.0") @Column(precision = 4, scale = 2)
    private BigDecimal notaFinal;

    private LocalDate fechaResultado;
}

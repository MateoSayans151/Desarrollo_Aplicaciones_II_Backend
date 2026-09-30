package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "cursos", uniqueConstraints = @UniqueConstraint(columnNames = {"periodo_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Curso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank @Column(nullable = false, length = 30)
    private String codigo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoAcademico periodo;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ModalidadCurso modalidad;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private EstadoCurso estado = EstadoCurso.PLANIFICADO;

    @Positive @Column(nullable = false)
    private int cupoMaximo;

    @NotNull @Column(nullable = false)
    private LocalDate fechaInicio;

    @NotNull @Column(nullable = false)
    private LocalDate fechaFin;
}

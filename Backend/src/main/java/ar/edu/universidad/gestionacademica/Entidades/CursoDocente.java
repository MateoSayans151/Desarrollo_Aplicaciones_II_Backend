package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "curso_docentes", uniqueConstraints = @UniqueConstraint(columnNames = {"curso_id", "docente_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoDocente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    // Identificador del módulo de docentes/identidad; no se replica su información personal.
    @NotBlank @Column(name = "docente_id", nullable = false, length = 100)
    private String docenteId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RolDocenteCurso rol;
}

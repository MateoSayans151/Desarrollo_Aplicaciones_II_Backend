package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "correlatividades", uniqueConstraints = @UniqueConstraint(columnNames = {"asignatura_id", "correlativa_id", "tipo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Correlatividad {
    public enum Tipo { REGULAR, APROBADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "correlativa_id", nullable = false)
    private Asignatura correlativa;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Tipo tipo;
}

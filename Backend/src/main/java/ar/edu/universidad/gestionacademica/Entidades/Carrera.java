package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "carreras", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Carrera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 20)
    private String codigo;
    @NotBlank @Column(nullable = false)
    private String nombre;
    @NotBlank @Column(nullable = false)
    private String facultad;
    @Column(nullable = false)
    @Builder.Default
    private String duracion = "0 años";
    @NotBlank @Column(nullable = false)
    private String titulo;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private EstadoAcademico estado = EstadoAcademico.ACTIVA;
}

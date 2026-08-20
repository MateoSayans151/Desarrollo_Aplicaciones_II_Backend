package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "sedes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sede {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, unique = true)
    private String nombre;
    @NotBlank @Column(nullable = false)
    private String direccion;
}

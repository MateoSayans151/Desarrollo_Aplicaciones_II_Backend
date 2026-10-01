package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

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
    /** UUID de la sede en Backoffice; el id Long sigue siendo la clave local. */
    @Column(name = "backoffice_site_id", unique = true)
    private UUID backofficeSiteId;
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'ACTIVE'")
    @Builder.Default
    private String estado = "ACTIVE";
}

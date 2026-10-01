package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "aulas", uniqueConstraints = @UniqueConstraint(columnNames = {"sede_id", "codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Aula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, length = 30)
    private String codigo;
    @NotBlank @Column(nullable = false, length = 255, columnDefinition = "varchar(255) default ''")
    private String nombre;
    /** UUID de la ubicación en Backoffice; puede ser nulo mientras sea sólo local. */
    @Column(name = "backoffice_location_id", unique = true)
    private UUID backofficeLocationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'AULA'")
    @Builder.Default
    private TipoUbicacion tipo = TipoUbicacion.AULA;
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30) default 'ACTIVE'")
    @Builder.Default
    private String estado = "ACTIVE";
    @Positive @Column(nullable = false)
    private int capacidadMaxima;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
}

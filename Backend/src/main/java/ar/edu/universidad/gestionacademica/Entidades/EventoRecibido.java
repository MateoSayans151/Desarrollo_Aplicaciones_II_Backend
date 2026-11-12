package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Inbox de eventos consumidos desde CORE. La clave es el eventId: un evento repetido
 * se reconoce y se descarta sin volver a aplicar su efecto.
 */
@Entity
@Table(name = "eventos_recibidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoRecibido {
    @Id @Column(name = "event_id", length = 100)
    private String eventId;
    @Column(nullable = false, length = 150)
    private String tipo;
    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;
}

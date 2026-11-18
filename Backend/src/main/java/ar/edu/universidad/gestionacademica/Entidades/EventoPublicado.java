package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Outbox de eventos academic.* para CORE. Se guarda en la misma transacción que el cambio que
 * lo origina y un proceso en segundo plano lo envía a POST /internal/v1/events. Reenviar el
 * mismo eventId con el mismo sobre es seguro: CORE no lo duplica.
 */
@Entity
@Table(name = "eventos_publicados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoPublicado {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String ENVIADO = "ENVIADO";
    public static final String RECHAZADO = "RECHAZADO";

    @Id @Column(name = "event_id", length = 36)
    private String eventId;
    @Column(nullable = false, length = 150)
    private String tipo;
    /** Envelope completo en JSON, tal como se envía. */
    @Column(nullable = false, columnDefinition = "text")
    private String sobre;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = PENDIENTE;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
    @Column(name = "enviado_en")
    private Instant enviadoEn;
    @Column(nullable = false)
    private int intentos;
    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;
}

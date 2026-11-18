package ar.edu.universidad.gestionacademica.Integracion.Core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Publicación de eventos academic.* en CORE.
 *
 * @param publishEnabled con true se registran los eventos en el outbox y se envían a CORE
 * @param url            POST /internal/v1/events de CORE
 * @param sendInterval   cada cuánto se envían los pendientes
 * @param maxAttempts    intentos antes de dar por rechazado un evento
 */
@ConfigurationProperties(prefix = "core.events")
public record PropiedadesEventosCore(
        boolean publishEnabled,
        String url,
        Duration sendInterval,
        int maxAttempts) {

    public PropiedadesEventosCore {
        if (sendInterval == null) sendInterval = Duration.ofSeconds(15);
        if (maxAttempts < 1) maxAttempts = 50;
    }
}

package ar.edu.universidad.gestionacademica.Integracion.Core;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales del service account academic-service para llamar a otros módulos.
 *
 * @param tokenUrl     endpoint de CORE que emite tokens de servicio (client credentials)
 * @param clientId     clientId del service account
 * @param clientSecret secreto del service account; sólo por variable de entorno
 * @param token        token fijo opcional; tiene prioridad sobre tokenUrl. Sirve para probar
 *                     contra despliegues que validan tokens del CORE simulado
 */
@ConfigurationProperties(prefix = "core.service")
public record PropiedadesServicioCore(
        String tokenUrl,
        String clientId,
        String clientSecret,
        String token) {
}

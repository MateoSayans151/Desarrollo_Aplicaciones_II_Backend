package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Integración con Backoffice (G8), dueño del catálogo de sedes y espacios.
 *
 * @param enabled       con true se sincroniza el catálogo y se bloquea el alta local de sedes y aulas
 * @param baseUrl       URL base del backend de Backoffice (rutas /internal/v1)
 * @param sitesPath     ruta interna de sedes
 * @param locationsPath ruta interna de espacios
 * @param pageSize      tamaño de página pedido (Backoffice admite hasta 100)
 * @param events        consumo de backoffice.location.upserted.v1 desde la cola de CORE
 */
@ConfigurationProperties(prefix = "backoffice")
public record PropiedadesBackoffice(
        boolean enabled,
        String baseUrl,
        String sitesPath,
        String locationsPath,
        int pageSize,
        Eventos events) {

    public PropiedadesBackoffice {
        if (sitesPath == null || sitesPath.isBlank()) sitesPath = "/internal/v1/sites";
        if (locationsPath == null || locationsPath.isBlank()) locationsPath = "/internal/v1/locations";
        if (pageSize < 1 || pageSize > 100) pageSize = 100;
        if (events == null) events = new Eventos(false, "academic.locations.v1");
    }

    public record Eventos(boolean enabled, String queue) { }
}

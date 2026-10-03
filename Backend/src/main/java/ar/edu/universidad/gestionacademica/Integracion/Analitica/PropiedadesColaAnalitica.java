package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics.queue")
public record PropiedadesColaAnalitica(
        boolean enabled,
        String exchange,
        String queue,
        String routingKey) { }

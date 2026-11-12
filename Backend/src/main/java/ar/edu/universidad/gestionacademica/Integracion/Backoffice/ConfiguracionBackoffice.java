package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Integracion.Core.PropiedadesServicioCore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableConfigurationProperties({PropiedadesBackoffice.class, PropiedadesServicioCore.class})
public class ConfiguracionBackoffice {

    /** Con la integración activa, el catálogo se lee al iniciar y luego de forma periódica. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "backoffice", name = "enabled", havingValue = "true")
    static class SincronizacionPeriodica {
        private final SincronizacionBackofficeServicio servicio;

        SincronizacionPeriodica(SincronizacionBackofficeServicio servicio) {
            this.servicio = servicio;
        }

        @Scheduled(initialDelayString = "${backoffice.sync-initial-delay:PT20S}",
                fixedDelayString = "${backoffice.sync-interval:PT6H}")
        void sincronizar() {
            servicio.sincronizarSinPropagar();
        }
    }
}

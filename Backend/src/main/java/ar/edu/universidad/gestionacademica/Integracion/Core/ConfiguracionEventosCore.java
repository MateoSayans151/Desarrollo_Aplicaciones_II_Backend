package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Repositorios.EventoPublicadoRepositorio;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PropiedadesEventosCore.class)
public class ConfiguracionEventosCore {

    /** Con CORE_EVENTS_PUBLISH_ENABLED=true el outbox se envía a CORE de forma periódica. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "core.events", name = "publish-enabled", havingValue = "true")
    static class EnvioPeriodico {
        private final EnviadorEventosCore enviador;

        EnvioPeriodico(EnviadorEventosCore enviador) {
            this.enviador = enviador;
        }

        @Scheduled(initialDelayString = "PT15S", fixedDelayString = "${core.events.send-interval:PT15S}")
        void enviar() {
            enviador.enviarPendientes();
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "core.events", name = "publish-enabled", havingValue = "true")
    EnviadorEventosCore enviadorEventosCore(PropiedadesEventosCore propiedades, EventoPublicadoRepositorio outbox,
                                            ProveedorTokenServicio tokens, RestClient.Builder builder) {
        return new EnviadorEventosCore(propiedades, outbox, tokens, builder);
    }
}

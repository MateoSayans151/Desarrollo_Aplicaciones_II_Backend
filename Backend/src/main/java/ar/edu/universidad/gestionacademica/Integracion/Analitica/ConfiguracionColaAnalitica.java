package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
@ConditionalOnProperty(prefix = "analytics.queue", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PropiedadesColaAnalitica.class)
public class ConfiguracionColaAnalitica {
    @Bean
    Queue colaAnalitica(PropiedadesColaAnalitica propiedades) {
        return new Queue(propiedades.queue(), true);
    }

    @Bean
    DirectExchange exchangeAnalitica(PropiedadesColaAnalitica propiedades) {
        return new DirectExchange(propiedades.exchange(), true, false);
    }

    @Bean
    Binding enlaceColaAnalitica(Queue colaAnalitica, DirectExchange exchangeAnalitica,
                                PropiedadesColaAnalitica propiedades) {
        return BindingBuilder.bind(colaAnalitica).to(exchangeAnalitica).with(propiedades.routingKey());
    }

    @Bean
    MessageConverter conversorJsonEventosAnalitica() {
        return new Jackson2JsonMessageConverter();
    }
}

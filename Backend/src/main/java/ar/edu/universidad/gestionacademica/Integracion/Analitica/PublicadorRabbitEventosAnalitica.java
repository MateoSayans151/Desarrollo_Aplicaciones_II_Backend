package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "analytics.queue", name = "enabled", havingValue = "true")
public class PublicadorRabbitEventosAnalitica implements PublicadorEventosAnalitica {
    private final RabbitTemplate rabbitTemplate;
    private final PropiedadesColaAnalitica propiedades;

    @Override
    public void publicar(EventoNotificacionDto evento) {
        rabbitTemplate.convertAndSend(propiedades.exchange(), propiedades.routingKey(), evento);
    }
}

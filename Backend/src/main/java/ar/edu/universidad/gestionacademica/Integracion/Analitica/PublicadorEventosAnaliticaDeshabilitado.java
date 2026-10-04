package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
//@ConditionalOnMissingBean(PublicadorEventosAnalitica.class)
@ConditionalOnProperty(
    prefix = "analytics.queue",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class PublicadorEventosAnaliticaDeshabilitado implements PublicadorEventosAnalitica {
    @Override
    public void publicar(EventoNotificacionDto evento) {
        // La cola esta explicitamente deshabilitada.
    }
}

package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PublicadorEventosAnalitica.class)
public class PublicadorEventosAnaliticaDeshabilitado implements PublicadorEventosAnalitica {
    @Override
    public void publicar(EventoNotificacionDto evento) {
        // La cola esta explicitamente deshabilitada.
    }
}

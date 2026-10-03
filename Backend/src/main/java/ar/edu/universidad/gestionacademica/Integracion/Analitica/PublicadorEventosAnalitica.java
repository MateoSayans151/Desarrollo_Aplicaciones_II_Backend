package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;

public interface PublicadorEventosAnalitica {
    void publicar(EventoNotificacionDto evento);
}

package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import ar.edu.universidad.gestionacademica.Modelos.EventoNotificacionDto;

/** Evento interno; se despacha al broker solo despues de confirmar la transaccion. */
public record EventoResultadoPublicado(EventoNotificacionDto evento) { }

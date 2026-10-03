package ar.edu.universidad.gestionacademica.Integracion.Analitica;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DespachadorEventosAnalitica {
    private final PublicadorEventosAnalitica publicador;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void despachar(EventoResultadoPublicado evento) {
        publicador.publicar(evento.evento());
    }
}

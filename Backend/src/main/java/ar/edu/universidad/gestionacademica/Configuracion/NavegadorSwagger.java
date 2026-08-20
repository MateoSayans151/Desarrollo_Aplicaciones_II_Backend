package ar.edu.universidad.gestionacademica.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
@ConditionalOnProperty(name = "universidad.abrir-swagger-al-iniciar", havingValue = "true", matchIfMissing = true)
public class NavegadorSwagger {
    private static final Logger log = LoggerFactory.getLogger(NavegadorSwagger.class);

    @Value("${server.port:8080}")
    private String puerto;

    @EventListener(ApplicationReadyEvent.class)
    public void abrirSwagger() {
        String url = "http://localhost:" + puerto + "/swagger-ui.html";
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("No se pudo abrir el navegador automaticamente (entorno sin escritorio). Abri manualmente: {}", url);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            log.warn("No se pudo abrir el navegador automaticamente en {}. Abrilo manualmente.", url, ex);
        }
    }
}

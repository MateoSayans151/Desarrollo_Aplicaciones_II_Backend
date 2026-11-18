package ar.edu.universidad.gestionacademica.Integracion.Core;

import ar.edu.universidad.gestionacademica.Entidades.EventoPublicado;
import ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException;
import ar.edu.universidad.gestionacademica.Repositorios.EventoPublicadoRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Envía a CORE los eventos pendientes del outbox, en orden de creación.
 * <ul>
 *   <li>202: enviado.</li>
 *   <li>409 (mismo eventId con otro contenido) u otro 4xx de validación: rechazado, no se reintenta.</li>
 *   <li>401, 403, 429, 5xx o error de red: se reintenta en la próxima pasada y se corta la actual
 *       para no desordenar los eventos.</li>
 * </ul>
 */
public class EnviadorEventosCore {
    private static final Logger log = LoggerFactory.getLogger(EnviadorEventosCore.class);
    private static final Set<HttpStatus> TRANSITORIOS = Set.of(
            HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.REQUEST_TIMEOUT, HttpStatus.TOO_MANY_REQUESTS);

    private final PropiedadesEventosCore propiedades;
    private final EventoPublicadoRepositorio outbox;
    private final ProveedorTokenServicio tokens;
    private final RestClient http;
    private final Clock reloj;
    private final ReentrantLock enCurso = new ReentrantLock();

    @Autowired
    public EnviadorEventosCore(PropiedadesEventosCore propiedades, EventoPublicadoRepositorio outbox,
                               ProveedorTokenServicio tokens, RestClient.Builder builder) {
        this(propiedades, outbox, tokens, builder.requestFactory(fabricaConTimeouts()).build(), Clock.systemUTC());
    }

    EnviadorEventosCore(PropiedadesEventosCore propiedades, EventoPublicadoRepositorio outbox,
                        ProveedorTokenServicio tokens, RestClient http, Clock reloj) {
        this.propiedades = propiedades;
        this.outbox = outbox;
        this.tokens = tokens;
        this.http = http;
        this.reloj = reloj;
    }

    private static SimpleClientHttpRequestFactory fabricaConTimeouts() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(10_000);
        return fabrica;
    }

    public void enviarPendientes() {
        if (!enCurso.tryLock()) return;
        try {
            for (EventoPublicado evento : outbox.findTop20ByEstadoOrderByCreadoEnAsc(EventoPublicado.PENDIENTE)) {
                if (!enviar(evento)) break;
            }
        } catch (RuntimeException ex) {
            log.warn("No se pudieron enviar los eventos pendientes a CORE: {}", ex.getMessage());
        } finally {
            enCurso.unlock();
        }
    }

    /** @return false si conviene cortar la pasada (error transitorio) */
    boolean enviar(EventoPublicado evento) {
        evento.setIntentos(evento.getIntentos() + 1);
        try {
            http.post().uri(propiedades.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.token())
                    .body(evento.getSobre())
                    .retrieve()
                    .toBodilessEntity();
            evento.setEstado(EventoPublicado.ENVIADO);
            evento.setEnviadoEn(Instant.now(reloj));
            evento.setUltimoError(null);
            outbox.save(evento);
            log.info("Evento {} {} publicado en CORE", evento.getTipo(), evento.getEventId());
            return true;
        } catch (HttpClientErrorException ex) {
            HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
            if (estado == HttpStatus.UNAUTHORIZED) tokens.invalidar();
            if (estado != null && TRANSITORIOS.contains(estado)) {
                return reintentar(evento, ex.getStatusCode() + " " + ex.getResponseBodyAsString());
            }
            evento.setEstado(EventoPublicado.RECHAZADO);
            evento.setUltimoError(recortar(ex.getStatusCode() + " " + ex.getResponseBodyAsString()));
            outbox.save(evento);
            log.error("CORE rechazó el evento {} {}: {}", evento.getTipo(), evento.getEventId(), evento.getUltimoError());
            return true;
        } catch (RestClientException | IntegracionExternaException ex) {
            return reintentar(evento, ex.getMessage());
        }
    }

    private boolean reintentar(EventoPublicado evento, String error) {
        evento.setUltimoError(recortar(error));
        if (evento.getIntentos() >= propiedades.maxAttempts()) {
            evento.setEstado(EventoPublicado.RECHAZADO);
            log.error("Se agotaron los intentos para el evento {} {}: {}", evento.getTipo(), evento.getEventId(), error);
        } else {
            log.warn("No se pudo enviar el evento {} {} (intento {}): {}", evento.getTipo(), evento.getEventId(),
                    evento.getIntentos(), error);
        }
        outbox.save(evento);
        return false;
    }

    private static String recortar(String valor) {
        if (valor == null) return null;
        return valor.length() <= 500 ? valor : valor.substring(0, 500);
    }
}

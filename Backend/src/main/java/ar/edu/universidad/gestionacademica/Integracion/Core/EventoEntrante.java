package ar.edu.universidad.gestionacademica.Integracion.Core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Evento recibido de una cola de CORE. El envelope (specVersion 1.0) trae eventId, eventType
 * (igual a la routing key), source, occurredAt, subject, correlationId, causationId, actor y data.
 *
 * @param eventId id del evento, recortado a 100 caracteres (largo de la columna del inbox)
 * @param tipo    eventType, o la routing key si el mensaje no lo trae
 * @param data    payload del evento
 * @param cuerpo  mensaje completo tal como llegó, para guardarlo y reprocesarlo
 */
public record EventoEntrante(String eventId, String tipo, JsonNode data, String cuerpo) {
    private static final int LARGO_EVENT_ID = 100;

    public static EventoEntrante leer(ObjectMapper json, Message mensaje) throws IOException {
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        JsonNode raiz = json.readTree(cuerpo);
        String tipo = texto(raiz, "eventType", "type");
        if (tipo == null) tipo = mensaje.getMessageProperties().getReceivedRoutingKey();
        String eventId = texto(raiz, "eventId", "id");
        if (eventId == null) eventId = mensaje.getMessageProperties().getMessageId();
        if (eventId != null && eventId.length() > LARGO_EVENT_ID) eventId = eventId.substring(0, LARGO_EVENT_ID);
        JsonNode data = raiz.hasNonNull("data") && raiz.get("data").isObject() ? raiz.get("data") : raiz;
        return new EventoEntrante(eventId, tipo, data, cuerpo);
    }

    public static String texto(JsonNode nodo, String... campos) {
        if (nodo == null) return null;
        for (String campo : campos) {
            if (nodo.hasNonNull(campo) && !nodo.get(campo).asText().isBlank()) return nodo.get(campo).asText().trim();
        }
        return null;
    }
}

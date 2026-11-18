-- Fase 3: eventos de CORE (RabbitMQ y publicación HTTP). Idempotente; sirve para Supabase y
-- volúmenes locales. Con DDL_AUTO=update Hibernate lo crea solo, pero el perfil local valida.

-- Eventos pendientes de aplicar (Docencia) o fallidos: se guardan para reprocesarlos.
ALTER TABLE eventos_recibidos ADD COLUMN IF NOT EXISTS payload TEXT;
ALTER TABLE eventos_recibidos ADD COLUMN IF NOT EXISTS error VARCHAR(500);

-- Outbox de eventos academic.* que se publican en CORE (POST /internal/v1/events).
CREATE TABLE IF NOT EXISTS eventos_publicados (
    event_id VARCHAR(36) PRIMARY KEY,
    tipo VARCHAR(150) NOT NULL,
    sobre TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    creado_en TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    enviado_en TIMESTAMP(6) WITH TIME ZONE,
    intentos INTEGER NOT NULL,
    ultimo_error VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_eventos_publicados_estado ON eventos_publicados (estado, creado_en);

-- Fase 3: integración con Backoffice (G8). Idempotente; sirve para Supabase y volúmenes locales.
-- Con DDL_AUTO=update Hibernate crea la tabla solo, pero el perfil local valida el esquema.

-- Inbox de eventos consumidos desde CORE (backoffice.location.upserted.v1, etc.).
CREATE TABLE IF NOT EXISTS eventos_recibidos (
    event_id VARCHAR(100) PRIMARY KEY,
    tipo VARCHAR(150) NOT NULL,
    recibido_en TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

-- Las columnas backoffice_site_id y backoffice_location_id ya existen desde la migración 02.
-- aulas.tipo es VARCHAR sin check, así que admite los tipos nuevos de Backoffice
-- (LABORATORIO, SALA_EVENTOS, OFICINA, DEPOSITO, OTRO) sin cambios.

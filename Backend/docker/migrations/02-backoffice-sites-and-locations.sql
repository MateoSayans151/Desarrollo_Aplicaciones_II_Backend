-- Migración manual para bases existentes antes del soporte de Backoffice.
-- Las columnas UUID son nullable para permitir sedes/aulas locales sin origen externo.

ALTER TABLE sedes
    ADD COLUMN IF NOT EXISTS backoffice_site_id UUID,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sedes_backoffice_site_id
    ON sedes (backoffice_site_id);

ALTER TABLE aulas
    ADD COLUMN IF NOT EXISTS nombre VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS backoffice_location_id UUID,
    ADD COLUMN IF NOT EXISTS tipo VARCHAR(30) NOT NULL DEFAULT 'AULA',
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

UPDATE aulas
SET nombre = codigo
WHERE nombre = '';

CREATE UNIQUE INDEX IF NOT EXISTS uk_aulas_backoffice_location_id
    ON aulas (backoffice_location_id);

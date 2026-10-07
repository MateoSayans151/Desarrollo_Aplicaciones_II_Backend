-- Los cursos existentes quedan con los tres campos nulos hasta que se conozca su horario.
-- Los cursos nuevos deben enviarlos mediante la API.
ALTER TABLE cursos
    ADD COLUMN IF NOT EXISTS turno VARCHAR(20),
    ADD COLUMN IF NOT EXISTS hora_inicio TIME,
    ADD COLUMN IF NOT EXISTS hora_fin TIME;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cursos_turno') THEN
        ALTER TABLE cursos ADD CONSTRAINT ck_cursos_turno
            CHECK (turno IS NULL OR turno IN ('TARDE', 'MAÑANA', 'NOCHE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cursos_horas') THEN
        ALTER TABLE cursos ADD CONSTRAINT ck_cursos_horas
            CHECK ((hora_inicio IS NULL AND hora_fin IS NULL)
                OR (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_fin > hora_inicio));
    END IF;
END $$;

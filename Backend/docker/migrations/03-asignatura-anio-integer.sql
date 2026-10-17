-- Migra el año de cursada a INTEGER para alinearlo con la entidad Asignatura.
-- Admite tanto valores numéricos como textos heredados: "1", "1er anio",
-- "2do año", etc. La migración se detiene si encuentra un valor sin dígitos.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM asignaturas
        WHERE anio IS NULL OR BTRIM(anio) !~ '[0-9]+'
    ) THEN
        RAISE EXCEPTION 'Hay valores de asignaturas.anio que no contienen un año numérico';
    END IF;
END $$;

ALTER TABLE asignaturas
    ALTER COLUMN anio TYPE INTEGER
    USING (SUBSTRING(BTRIM(anio) FROM '[0-9]+'))::INTEGER;

-- Migra el año de cursada a INTEGER para alinearlo con la entidad Asignatura.
-- Los valores actuales deben contener únicamente números (por ejemplo, "1" o "2").

ALTER TABLE asignaturas
    ALTER COLUMN anio TYPE INTEGER
    USING NULLIF(BTRIM(anio), '')::INTEGER;

-- Ejecutar una sola vez sobre bases existentes antes de iniciar la nueva versión.
-- No se elige un horario arbitrario si un curso tiene más de uno.
BEGIN;

ALTER TABLE cursos
    ADD COLUMN IF NOT EXISTS dia_semana VARCHAR(20),
    ADD COLUMN IF NOT EXISTS aula_id BIGINT REFERENCES aulas(id);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM horarios_curso GROUP BY curso_id HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Hay cursos con más de un horario: resolverlos antes de migrar';
    END IF;
END $$;

UPDATE cursos c
SET dia_semana = h.dia_semana,
    aula_id = h.aula_id,
    hora_inicio = h.hora_inicio,
    hora_fin = h.hora_fin
FROM horarios_curso h
WHERE h.curso_id = c.id;

DROP TABLE horarios_curso;

COMMIT;

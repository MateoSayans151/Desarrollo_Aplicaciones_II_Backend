-- Alinea bases creadas con versiones anteriores con las entidades actuales.
-- Es idempotente y se puede aplicar tanto en Supabase como en un volumen local.

BEGIN;

-- PlanEstudio guarda PLANIFICADO/ACTIVO; el check anterior sólo admitía
-- ACTIVA/BORRADOR/INACTIVA y hacía fallar la creación de cualquier plan.
ALTER TABLE planes_estudio DROP CONSTRAINT IF EXISTS planes_estudio_estado_check;

UPDATE planes_estudio SET estado = 'ACTIVO' WHERE estado = 'ACTIVA';
UPDATE planes_estudio SET estado = 'PLANIFICADO' WHERE estado IN ('BORRADOR', 'INACTIVA');

ALTER TABLE planes_estudio
    ADD CONSTRAINT planes_estudio_estado_check CHECK (estado IN ('PLANIFICADO', 'ACTIVO'));

-- Una materia sólo puede ser correlativa de otra una vez, cualquiera sea el tipo.
-- Si hubiera dos filas para el mismo par se conserva la más exigente (APROBADA).
DELETE FROM correlatividades repetida
USING correlatividades conservada
WHERE repetida.asignatura_id = conservada.asignatura_id
  AND repetida.correlativa_id = conservada.correlativa_id
  AND repetida.id <> conservada.id
  AND (conservada.tipo = 'APROBADA' AND repetida.tipo <> 'APROBADA'
       OR conservada.tipo = repetida.tipo AND conservada.id < repetida.id);

DO $$
DECLARE
    restriccion TEXT;
BEGIN
    FOR restriccion IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'correlatividades'::regclass
          AND contype = 'u'
          AND pg_get_constraintdef(oid) LIKE '%tipo%'
    LOOP
        EXECUTE format('ALTER TABLE correlatividades DROP CONSTRAINT %I', restriccion);
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'correlatividades'::regclass
          AND conname = 'uk_correlatividad_par'
    ) THEN
        ALTER TABLE correlatividades
            ADD CONSTRAINT uk_correlatividad_par UNIQUE (asignatura_id, correlativa_id);
    END IF;
END $$;

COMMIT;

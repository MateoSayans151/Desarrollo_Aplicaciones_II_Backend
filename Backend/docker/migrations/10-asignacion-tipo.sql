-- Una asignación de aula puede ser una CLASE o un EXAMEN. Los exámenes se publican en CORE
-- como academic.exam-scheduled.v1. Idempotente: las asignaciones existentes quedan como CLASE.
ALTER TABLE asignaciones_aula ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'CLASE';

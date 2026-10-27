-- Datos de prueba autocontenidos para Supabase.
-- Todos los registros propios usan el prefijo TST y las inserciones son idempotentes.
-- Se ajustan a las reglas de negocio vigentes: codigos con formato AAA-999,
-- correlatividades del mismo plan, cupos dentro de la capacidad y horarios sin solapamientos.

BEGIN;

INSERT INTO carreras (codigo, nombre, facultad, duracion, titulo, estado)
VALUES ('TST-001', 'Tecnicatura en Pruebas de Sistemas',
        'Facultad de Ingenieria y Ciencias Exactas', '2 anos',
        'Tecnico/a Universitario/a en Pruebas de Sistemas', 'ACTIVA')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO planes_estudio (codigo, nombre, vigencia_desde, cantidad_asignaturas, estado, carrera_id)
SELECT 'TST-002', 'Plan de estudios de Pruebas 2026', DATE '2026-01-01', 3, 'ACTIVA', c.id
FROM carreras c
WHERE c.codigo = 'TST-001'
ON CONFLICT (carrera_id, codigo) DO NOTHING;

INSERT INTO asignaturas (codigo, nombre, anio, creditos, carga_horaria, estado, plan_id)
SELECT d.codigo, d.nombre, d.anio, d.creditos, d.carga_horaria, 'ACTIVA', p.id
FROM planes_estudio p
CROSS JOIN (
    VALUES
        ('TST-101', 'Fundamentos de pruebas', 1, 6, 64),
        ('TST-102', 'Automatizacion de pruebas', 1, 6, 64),
        ('TST-201', 'Integracion y calidad continua', 2, 6, 64)
) AS d(codigo, nombre, anio, creditos, carga_horaria)
WHERE p.codigo = 'TST-002'
ON CONFLICT (plan_id, codigo) DO NOTHING;

INSERT INTO correlatividades (asignatura_id, correlativa_id, tipo)
SELECT destino.id, requerida.id, d.tipo
FROM (
    VALUES
        ('TST-102', 'TST-101', 'REGULAR'),
        ('TST-201', 'TST-102', 'APROBADA')
) AS d(asignatura_codigo, correlativa_codigo, tipo)
JOIN planes_estudio p ON p.codigo = 'TST-002'
JOIN asignaturas destino ON destino.codigo = d.asignatura_codigo AND destino.plan_id = p.id
JOIN asignaturas requerida ON requerida.codigo = d.correlativa_codigo AND requerida.plan_id = p.id
ON CONFLICT (asignatura_id, correlativa_id, tipo) DO NOTHING;

INSERT INTO sedes (nombre, direccion, estado)
VALUES ('Sede de Pruebas', 'Calle de pruebas 123, Buenos Aires', 'ACTIVE')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO aulas (codigo, nombre, tipo, capacidad_maxima, sede_id, estado)
SELECT d.codigo, d.nombre, d.tipo, d.capacidad, s.id, 'ACTIVE'
FROM (
    VALUES
        ('TST-A101', 'Aula de Pruebas 101', 'AULA', 40),
        ('TST-LAB1', 'Laboratorio de Pruebas', 'AULA', 20),
        ('TST-AUD1', 'Auditorio de Pruebas', 'AUDITORIO', 80)
) AS d(codigo, nombre, tipo, capacidad)
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
ON CONFLICT (sede_id, codigo) DO NOTHING;

-- El periodo historico permite probar notas y resultados. El siguiente mantiene
-- abierta la ventana de inscripcion desde siete dias antes hasta trece despues.
INSERT INTO periodos_academicos (anio, numero, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta)
VALUES (
    EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER - 1, 2,
    CURRENT_DATE - 400, CURRENT_DATE - 280,
    CURRENT_DATE - 450, CURRENT_DATE - 401
)
ON CONFLICT (anio, numero) DO NOTHING;

INSERT INTO periodos_academicos (anio, numero, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta)
VALUES (
    EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 2,
    CURRENT_DATE + 14, CURRENT_DATE + 140,
    CURRENT_DATE - 7, CURRENT_DATE + 13
)
ON CONFLICT (anio, numero) DO NOTHING;

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT 'TST-001', a.id, p.id, s.id, 'PRESENCIAL', 'FINALIZADO', 35,
       CURRENT_DATE - 390, CURRENT_DATE - 300, 'TARDE', TIME '14:00', TIME '17:00', 'MONDAY', aula.id
FROM asignaturas a
JOIN planes_estudio plan ON plan.id = a.plan_id AND plan.codigo = 'TST-002'
JOIN periodos_academicos p ON p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER - 1 AND p.numero = 2
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
JOIN aulas aula ON aula.sede_id = s.id AND aula.codigo = 'TST-A101'
WHERE a.codigo = 'TST-101'
ON CONFLICT (periodo_id, codigo) DO NOTHING;

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT 'TST-002', a.id, p.id, s.id, 'VIRTUAL', 'FINALIZADO', 18,
       CURRENT_DATE - 390, CURRENT_DATE - 300, 'TARDE', TIME '14:00', TIME '17:00', 'TUESDAY', aula.id
FROM asignaturas a
JOIN planes_estudio plan ON plan.id = a.plan_id AND plan.codigo = 'TST-002'
JOIN periodos_academicos p ON p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER - 1 AND p.numero = 2
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
JOIN aulas aula ON aula.sede_id = s.id AND aula.codigo = 'TST-LAB1'
WHERE a.codigo = 'TST-102'
ON CONFLICT (periodo_id, codigo) DO NOTHING;

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT 'TST-003', a.id, p.id, s.id, 'HIBRIDA', 'PLANIFICADO', 18,
       CURRENT_DATE + 21, CURRENT_DATE + 120, 'TARDE', TIME '17:00', TIME '20:00', 'WEDNESDAY', aula.id
FROM asignaturas a
JOIN planes_estudio plan ON plan.id = a.plan_id AND plan.codigo = 'TST-002'
JOIN periodos_academicos p ON p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER AND p.numero = 2
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
JOIN aulas aula ON aula.sede_id = s.id AND aula.codigo = 'TST-LAB1'
WHERE a.codigo = 'TST-102'
ON CONFLICT (periodo_id, codigo) DO NOTHING;

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT 'TST-004', a.id, p.id, s.id, 'PRESENCIAL', 'PLANIFICADO', 35,
       CURRENT_DATE + 21, CURRENT_DATE + 120, 'NOCHE', TIME '18:00', TIME '21:00', 'FRIDAY', aula.id
FROM asignaturas a
JOIN planes_estudio plan ON plan.id = a.plan_id AND plan.codigo = 'TST-002'
JOIN periodos_academicos p ON p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER AND p.numero = 2
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
JOIN aulas aula ON aula.sede_id = s.id AND aula.codigo = 'TST-A101'
WHERE a.codigo = 'TST-201'
ON CONFLICT (periodo_id, codigo) DO NOTHING;

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT 'TST-005', a.id, p.id, s.id, 'PRESENCIAL', 'CANCELADO', 35,
       CURRENT_DATE + 21, CURRENT_DATE + 120, 'TARDE', TIME '09:00', TIME '12:00', 'TUESDAY', aula.id
FROM asignaturas a
JOIN planes_estudio plan ON plan.id = a.plan_id AND plan.codigo = 'TST-002'
JOIN periodos_academicos p ON p.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER AND p.numero = 2
JOIN sedes s ON s.nombre = 'Sede de Pruebas'
JOIN aulas aula ON aula.sede_id = s.id AND aula.codigo = 'TST-A101'
WHERE a.codigo = 'TST-101'
ON CONFLICT (periodo_id, codigo) DO NOTHING;

INSERT INTO curso_docentes (curso_id, docente_id, rol)
SELECT c.id, d.docente_id, d.rol
FROM (
    VALUES
        ('TST-001', 'docente-test-001', 'TITULAR'),
        ('TST-002', 'docente-test-002', 'TITULAR'),
        ('TST-003', 'docente-test-001', 'TITULAR'),
        ('TST-003', 'docente-test-003', 'AUXILIAR'),
        ('TST-004', 'docente-test-004', 'ADJUNTO'),
        ('TST-005', 'docente-test-005', 'TITULAR')
) AS d(curso_codigo, docente_id, rol)
JOIN cursos c ON c.codigo = d.curso_codigo
ON CONFLICT (curso_id, docente_id) DO NOTHING;

INSERT INTO inscripciones_curso (curso_id, alumno_id, fecha_inscripcion, estado, nota_final, fecha_resultado)
SELECT c.id, d.alumno_id, d.fecha_inscripcion, d.estado, d.nota_final, d.fecha_resultado
FROM (
    VALUES
        ('TST-001', 'alumno-test-001', CURRENT_DATE - 430, 'APROBADO', 8.50::NUMERIC, CURRENT_DATE - 300),
        ('TST-001', 'alumno-test-002', CURRENT_DATE - 430, 'DESAPROBADO', 4.00::NUMERIC, CURRENT_DATE - 300),
        ('TST-002', 'alumno-test-003', CURRENT_DATE - 430, 'APROBADO', 9.00::NUMERIC, CURRENT_DATE - 300),
        ('TST-003', 'alumno-test-001', CURRENT_DATE, 'INSCRIPTO', NULL::NUMERIC, NULL::DATE),
        ('TST-004', 'alumno-test-003', CURRENT_DATE, 'INSCRIPTO', NULL::NUMERIC, NULL::DATE)
) AS d(curso_codigo, alumno_id, fecha_inscripcion, estado, nota_final, fecha_resultado)
JOIN cursos c ON c.codigo = d.curso_codigo
ON CONFLICT (curso_id, alumno_id) DO NOTHING;

INSERT INTO asignaciones_aula (aula_id, asignatura_id, fecha, hora_inicio, hora_fin, cantidad_estudiantes)
SELECT aula.id, a.id, CURRENT_DATE + 160, TIME '10:00', TIME '12:00', 30
FROM aulas aula
JOIN sedes s ON s.id = aula.sede_id AND s.nombre = 'Sede de Pruebas'
JOIN asignaturas a ON a.codigo = 'TST-101'
JOIN planes_estudio p ON p.id = a.plan_id AND p.codigo = 'TST-002'
WHERE aula.codigo = 'TST-A101'
  AND NOT EXISTS (
      SELECT 1
      FROM asignaciones_aula existente
      WHERE existente.aula_id = aula.id
        AND existente.fecha = CURRENT_DATE + 160
        AND existente.hora_inicio = TIME '10:00'
        AND existente.hora_fin = TIME '12:00'
  );

INSERT INTO turnos_examen (nombre, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta)
SELECT 'Turno final de prueba - proximo', CURRENT_DATE + 160, CURRENT_DATE + 175,
       CURRENT_DATE + 130, CURRENT_DATE + 150
WHERE NOT EXISTS (
    SELECT 1 FROM turnos_examen WHERE nombre = 'Turno final de prueba - proximo'
);

INSERT INTO turnos_examen (nombre, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta)
SELECT 'Turno final de prueba - cerrado', CURRENT_DATE - 80, CURRENT_DATE - 65,
       CURRENT_DATE - 110, CURRENT_DATE - 90
WHERE NOT EXISTS (
    SELECT 1 FROM turnos_examen WHERE nombre = 'Turno final de prueba - cerrado'
);

INSERT INTO regularidades_curso (curso_id, codigo, asistencia_minima, promedio_minimo)
SELECT c.id, c.codigo, 75.00, 6.00
FROM cursos c
WHERE NOT EXISTS (SELECT 1 FROM regularidades_curso r WHERE r.curso_id = c.id);

COMMIT;

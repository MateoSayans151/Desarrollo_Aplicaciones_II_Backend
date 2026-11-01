-- Datos de demostración coherentes con las reglas de negocio vigentes.
-- Requiere la migración 07 y las tablas académicas vacías; no toca la tabla usuarios.
--
-- Fechas pensadas para el 2.º cuatrimestre de 2026:
--   * Planes 2025 y 2026 ACTIVOS; plan 2027 PLANIFICADO (borrador de un plan futuro).
--   * Períodos 2025-2 y 2026-1 cerrados, 2026-2 en curso y 2027-1 con inscripción abierta.
--   * Sólo los planes activos tienen cursos y reservas de aula.

BEGIN;

INSERT INTO carreras (codigo, nombre, facultad, duracion, titulo, estado) VALUES
    ('INF-001', 'Ingeniería en Informática', 'Facultad de Ingeniería y Ciencias Exactas', '0 años', 'Ingeniero/a en Informática', 'ACTIVA'),
    ('ADM-001', 'Licenciatura en Administración', 'Facultad de Ciencias Económicas', '0 años', 'Licenciado/a en Administración', 'ACTIVA'),
    ('TEC-001', 'Tecnicatura en Programación', 'Facultad de Ingeniería y Ciencias Exactas', '0 años', 'Técnico/a en Programación', 'INACTIVA');

INSERT INTO planes_estudio (codigo, nombre, vigencia_desde, cantidad_asignaturas, estado, carrera_id)
SELECT d.codigo, d.nombre, d.vigencia::DATE, 0, d.estado, c.id
FROM (VALUES
    ('INF-001', 'INF-125', 'Plan de estudio 2025', '2025-03-01', 'ACTIVO'),
    ('INF-001', 'INF-126', 'Plan de estudio 2026', '2026-03-01', 'ACTIVO'),
    ('INF-001', 'INF-127', 'Plan de estudio 2027', '2027-03-01', 'PLANIFICADO'),
    ('ADM-001', 'ADM-125', 'Plan de estudio 2025', '2025-03-01', 'ACTIVO'),
    ('ADM-001', 'ADM-126', 'Plan de estudio 2026', '2026-03-01', 'ACTIVO'),
    ('ADM-001', 'ADM-127', 'Plan de estudio 2027', '2027-03-01', 'PLANIFICADO'),
    ('TEC-001', 'TEC-125', 'Plan de estudio 2025', '2025-03-01', 'ACTIVO')
) AS d(carrera, codigo, nombre, vigencia, estado)
JOIN carreras c ON c.codigo = d.carrera;

INSERT INTO asignaturas (codigo, nombre, anio, creditos, carga_horaria, estado, plan_id)
SELECT d.codigo, d.nombre, d.anio, d.creditos, d.carga, 'ACTIVA', p.id
FROM (VALUES
    ('INF-125', 'INF-101', 'Programación I', 1, 8, 98),
    ('INF-125', 'INF-102', 'Matemática I', 1, 8, 98),
    ('INF-125', 'INF-103', 'Programación II', 1, 8, 98),
    ('INF-125', 'INF-104', 'Matemática II', 1, 8, 98),
    ('INF-125', 'INF-105', 'Bases de Datos', 2, 10, 108),
    ('INF-125', 'INF-106', 'Sistemas Operativos', 2, 8, 98),
    ('INF-125', 'INF-107', 'Redes de Computadoras', 2, 8, 98),
    ('INF-125', 'INF-108', 'Ingeniería de Software', 2, 10, 108),

    ('INF-126', 'INF-101', 'Programación I', 1, 8, 98),
    ('INF-126', 'INF-102', 'Matemática I', 1, 8, 98),
    ('INF-126', 'INF-103', 'Programación II', 1, 8, 98),
    ('INF-126', 'INF-104', 'Matemática II', 1, 8, 98),
    ('INF-126', 'INF-105', 'Bases de Datos', 2, 10, 108),
    ('INF-126', 'INF-106', 'Sistemas Operativos', 2, 8, 98),
    ('INF-126', 'INF-107', 'Redes de Computadoras', 2, 8, 98),
    ('INF-126', 'INF-108', 'Ingeniería de Software', 2, 10, 108),
    ('INF-126', 'INF-109', 'Desarrollo de Aplicaciones I', 3, 10, 118),
    ('INF-126', 'INF-110', 'Desarrollo de Aplicaciones II', 3, 10, 118),
    ('INF-126', 'INF-111', 'Seguridad Informática', 3, 8, 98),
    ('INF-126', 'INF-112', 'Proyecto Final', 3, 20, 168),

    ('INF-127', 'INF-101', 'Programación I', 1, 8, 98),
    ('INF-127', 'INF-102', 'Matemática I', 1, 8, 98),
    ('INF-127', 'INF-103', 'Programación II', 1, 8, 98),
    ('INF-127', 'INF-104', 'Matemática II', 1, 8, 98),
    ('INF-127', 'INF-105', 'Bases de Datos', 2, 10, 108),
    ('INF-127', 'INF-106', 'Sistemas Operativos', 2, 8, 98),
    ('INF-127', 'INF-107', 'Redes de Computadoras', 2, 8, 98),
    ('INF-127', 'INF-108', 'Ingeniería de Software', 2, 10, 108),
    ('INF-127', 'INF-109', 'Desarrollo de Aplicaciones I', 3, 10, 118),
    ('INF-127', 'INF-110', 'Desarrollo de Aplicaciones II', 3, 10, 118),
    ('INF-127', 'INF-111', 'Seguridad Informática', 3, 8, 98),
    ('INF-127', 'INF-112', 'Ciencia de Datos', 3, 10, 108),
    ('INF-127', 'INF-113', 'Inteligencia Artificial', 4, 10, 118),
    ('INF-127', 'INF-114', 'Arquitectura en la Nube', 4, 8, 98),
    ('INF-127', 'INF-115', 'Proyecto Final', 4, 20, 168),

    ('ADM-125', 'ADM-101', 'Introducción a la Administración', 1, 6, 68),
    ('ADM-125', 'ADM-102', 'Contabilidad I', 1, 8, 98),
    ('ADM-125', 'ADM-103', 'Economía I', 1, 6, 68),
    ('ADM-125', 'ADM-104', 'Comercialización', 2, 8, 98),
    ('ADM-125', 'ADM-105', 'Costos', 2, 8, 98),

    ('ADM-126', 'ADM-101', 'Introducción a la Administración', 1, 6, 68),
    ('ADM-126', 'ADM-102', 'Contabilidad I', 1, 8, 98),
    ('ADM-126', 'ADM-103', 'Economía I', 1, 6, 68),
    ('ADM-126', 'ADM-104', 'Comercialización', 2, 8, 98),
    ('ADM-126', 'ADM-105', 'Costos', 2, 8, 98),
    ('ADM-126', 'ADM-106', 'Finanzas Corporativas', 2, 10, 108),

    ('ADM-127', 'ADM-101', 'Introducción a la Administración', 1, 6, 68),
    ('ADM-127', 'ADM-102', 'Contabilidad I', 1, 8, 98),
    ('ADM-127', 'ADM-103', 'Economía y Datos', 1, 6, 68),

    ('TEC-125', 'TEC-101', 'Fundamentos de Programación', 1, 8, 98),
    ('TEC-125', 'TEC-102', 'Algoritmos', 1, 8, 98),
    ('TEC-125', 'TEC-103', 'Programación Web', 2, 8, 98),
    ('TEC-125', 'TEC-104', 'Práctica Profesional', 2, 12, 118)
) AS d(plan, codigo, nombre, anio, creditos, carga)
JOIN planes_estudio p ON p.codigo = d.plan;

INSERT INTO correlatividades (asignatura_id, correlativa_id, tipo)
SELECT destino.id, requerida.id, d.tipo
FROM (VALUES
    ('INF-125', 'INF-103', 'INF-101', 'REGULAR'),
    ('INF-125', 'INF-104', 'INF-102', 'APROBADA'),
    ('INF-125', 'INF-105', 'INF-103', 'APROBADA'),
    ('INF-125', 'INF-108', 'INF-105', 'REGULAR'),

    ('INF-126', 'INF-103', 'INF-101', 'REGULAR'),
    ('INF-126', 'INF-104', 'INF-102', 'APROBADA'),
    ('INF-126', 'INF-105', 'INF-103', 'REGULAR'),
    ('INF-126', 'INF-106', 'INF-103', 'REGULAR'),
    ('INF-126', 'INF-107', 'INF-106', 'REGULAR'),
    ('INF-126', 'INF-108', 'INF-105', 'REGULAR'),
    ('INF-126', 'INF-109', 'INF-105', 'APROBADA'),
    ('INF-126', 'INF-109', 'INF-108', 'REGULAR'),
    ('INF-126', 'INF-110', 'INF-109', 'APROBADA'),
    ('INF-126', 'INF-111', 'INF-107', 'REGULAR'),
    ('INF-126', 'INF-112', 'INF-110', 'APROBADA'),
    ('INF-126', 'INF-112', 'INF-111', 'REGULAR'),

    ('INF-127', 'INF-103', 'INF-101', 'REGULAR'),
    ('INF-127', 'INF-104', 'INF-102', 'APROBADA'),
    ('INF-127', 'INF-105', 'INF-103', 'REGULAR'),
    ('INF-127', 'INF-109', 'INF-105', 'APROBADA'),
    ('INF-127', 'INF-110', 'INF-109', 'APROBADA'),
    ('INF-127', 'INF-113', 'INF-112', 'REGULAR'),
    ('INF-127', 'INF-115', 'INF-113', 'REGULAR'),

    ('ADM-125', 'ADM-105', 'ADM-102', 'APROBADA'),
    ('ADM-126', 'ADM-102', 'ADM-101', 'REGULAR'),
    ('ADM-126', 'ADM-105', 'ADM-102', 'APROBADA'),
    ('ADM-126', 'ADM-106', 'ADM-105', 'REGULAR'),
    ('ADM-127', 'ADM-102', 'ADM-101', 'REGULAR'),

    ('TEC-125', 'TEC-103', 'TEC-101', 'APROBADA')
) AS d(plan, asignatura, correlativa, tipo)
JOIN planes_estudio p ON p.codigo = d.plan
JOIN asignaturas destino ON destino.plan_id = p.id AND destino.codigo = d.asignatura
JOIN asignaturas requerida ON requerida.plan_id = p.id AND requerida.codigo = d.correlativa;

-- La cantidad de materias y la duración se derivan igual que en EstructuraAcademicaServicio.
UPDATE planes_estudio p
SET cantidad_asignaturas = (SELECT COUNT(*) FROM asignaturas a WHERE a.plan_id = p.id);

UPDATE carreras c
SET duracion = CASE
        WHEN s.semestres = 0 THEN '0 años'
        WHEN s.semestres = 2 THEN '1 año'
        WHEN s.semestres % 2 = 0 THEN (s.semestres / 2) || ' años'
        ELSE (s.semestres / 2) || ',5 años'
    END
FROM (
    SELECT carrera_id, (MAX(cantidad_asignaturas) + 4) / 5 AS semestres
    FROM planes_estudio GROUP BY carrera_id
) s
WHERE s.carrera_id = c.id;

INSERT INTO sedes (nombre, direccion, estado) VALUES
    ('Sede Monserrat', 'Lima 775, CABA', 'ACTIVE'),
    ('Sede Recoleta', 'Av. Pueyrredón 1950, CABA', 'ACTIVE'),
    ('Sede Pilar', 'Panamericana km 54,5, Pilar', 'ACTIVE');

INSERT INTO aulas (codigo, nombre, tipo, capacidad_maxima, sede_id, estado)
SELECT d.codigo, d.nombre, 'AULA', d.capacidad, s.id, 'ACTIVE'
FROM (VALUES
    ('Sede Monserrat', 'A-101', 'Aula 101', 40),
    ('Sede Monserrat', 'A-102', 'Aula 102', 30),
    ('Sede Monserrat', 'LAB-1', 'Laboratorio de Informática', 25),
    ('Sede Recoleta', 'R-201', 'Aula Magna Recoleta', 60),
    ('Sede Recoleta', 'R-202', 'Aula 202', 35),
    ('Sede Pilar', 'P-301', 'Aula 301', 50)
) AS d(sede, codigo, nombre, capacidad)
JOIN sedes s ON s.nombre = d.sede;

INSERT INTO periodos_academicos (anio, numero, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta) VALUES
    (2025, 2, '2025-08-11', '2025-12-06', '2025-07-07', '2025-08-08'),
    (2026, 1, '2026-03-09', '2026-07-04', '2026-02-09', '2026-03-06'),
    (2026, 2, '2026-08-10', '2026-12-05', '2026-07-06', '2026-08-07'),
    (2027, 1, '2027-03-08', '2027-07-03', '2026-10-19', '2027-03-05');

INSERT INTO turnos_examen (nombre, fecha_inicio, fecha_fin, inscripcion_desde, inscripcion_hasta) VALUES
    ('Turno Diciembre 2025', '2025-12-09', '2025-12-20', '2025-11-24', '2025-12-05'),
    ('Turno Febrero 2026', '2026-02-16', '2026-02-27', '2026-02-02', '2026-02-13'),
    ('Turno Julio 2026', '2026-07-06', '2026-07-18', '2026-06-22', '2026-07-03'),
    ('Turno Diciembre 2026', '2026-12-07', '2026-12-19', '2026-11-16', '2026-12-04'),
    ('Turno Febrero 2027', '2027-02-15', '2027-02-26', '2027-02-01', '2027-02-12');

INSERT INTO cursos (codigo, asignatura_id, periodo_id, sede_id, modalidad, estado, cupo_maximo,
                    fecha_inicio, fecha_fin, turno, hora_inicio, hora_fin, dia_semana, aula_id)
SELECT d.codigo, a.id, pe.id, s.id, d.modalidad, d.estado, d.cupo, pe.fecha_inicio, pe.fecha_fin,
       d.turno, d.inicio::TIME, d.fin::TIME, d.dia, au.id
FROM (VALUES
    ('CUR-251', 'INF-125', 'INF-101', 2025, 2, 'Sede Monserrat', 'A-101', 'PRESENCIAL', 'FINALIZADO', 30, 'MAÑANA', '08:00', '12:00', 'MONDAY'),
    ('CUR-252', 'INF-125', 'INF-102', 2025, 2, 'Sede Monserrat', 'A-101', 'PRESENCIAL', 'FINALIZADO', 30, 'TARDE', '14:00', '18:00', 'TUESDAY'),
    ('CUR-261', 'INF-126', 'INF-101', 2026, 1, 'Sede Monserrat', 'A-101', 'PRESENCIAL', 'FINALIZADO', 30, 'MAÑANA', '08:00', '12:00', 'MONDAY'),
    ('CUR-262', 'INF-126', 'INF-102', 2026, 1, 'Sede Monserrat', 'A-102', 'PRESENCIAL', 'FINALIZADO', 25, 'TARDE', '14:00', '18:00', 'TUESDAY'),
    ('CUR-263', 'ADM-126', 'ADM-101', 2026, 1, 'Sede Recoleta', 'R-201', 'PRESENCIAL', 'FINALIZADO', 40, 'NOCHE', '19:00', '22:00', 'WEDNESDAY'),
    ('CUR-271', 'INF-126', 'INF-103', 2026, 2, 'Sede Monserrat', 'LAB-1', 'PRESENCIAL', 'EN_CURSO', 25, 'MAÑANA', '08:00', '12:00', 'MONDAY'),
    ('CUR-272', 'INF-126', 'INF-104', 2026, 2, 'Sede Monserrat', 'A-102', 'PRESENCIAL', 'EN_CURSO', 25, 'TARDE', '14:00', '18:00', 'TUESDAY'),
    ('CUR-273', 'INF-126', 'INF-105', 2026, 2, 'Sede Recoleta', 'R-202', 'HIBRIDA', 'EN_CURSO', 30, 'NOCHE', '18:30', '22:00', 'THURSDAY'),
    ('CUR-274', 'ADM-126', 'ADM-102', 2026, 2, 'Sede Recoleta', 'R-201', 'PRESENCIAL', 'EN_CURSO', 35, 'NOCHE', '19:00', '22:00', 'WEDNESDAY'),
    ('CUR-275', 'INF-126', 'INF-106', 2026, 2, 'Sede Monserrat', 'A-101', 'PRESENCIAL', 'CANCELADO', 30, 'TARDE', '14:00', '18:00', 'FRIDAY'),
    ('CUR-281', 'INF-126', 'INF-101', 2027, 1, 'Sede Monserrat', 'A-101', 'PRESENCIAL', 'PLANIFICADO', 30, 'MAÑANA', '08:00', '12:00', 'MONDAY'),
    ('CUR-282', 'INF-126', 'INF-103', 2027, 1, 'Sede Monserrat', 'LAB-1', 'PRESENCIAL', 'PLANIFICADO', 25, 'TARDE', '14:00', '18:00', 'WEDNESDAY'),
    ('CUR-283', 'ADM-126', 'ADM-101', 2027, 1, 'Sede Recoleta', 'R-201', 'PRESENCIAL', 'PLANIFICADO', 40, 'NOCHE', '19:00', '22:00', 'WEDNESDAY'),
    ('CUR-284', 'INF-126', 'INF-105', 2027, 1, 'Sede Pilar', 'P-301', 'VIRTUAL', 'PLANIFICADO', 35, 'MAÑANA', '09:00', '12:00', 'SATURDAY')
) AS d(codigo, plan, asignatura, anio, numero, sede, aula, modalidad, estado, cupo, turno, inicio, fin, dia)
JOIN planes_estudio p ON p.codigo = d.plan
JOIN asignaturas a ON a.plan_id = p.id AND a.codigo = d.asignatura
JOIN periodos_academicos pe ON pe.anio = d.anio AND pe.numero = d.numero
JOIN sedes s ON s.nombre = d.sede
JOIN aulas au ON au.sede_id = s.id AND au.codigo = d.aula;

INSERT INTO regularidades_curso (curso_id, codigo, asistencia_minima, promedio_minimo)
SELECT c.id, c.codigo,
       CASE WHEN c.modalidad = 'VIRTUAL' THEN 60.00 WHEN c.modalidad = 'HIBRIDA' THEN 70.00 ELSE 75.00 END,
       CASE WHEN c.codigo IN ('CUR-272', 'CUR-282') THEN 7.00 ELSE 6.00 END
FROM cursos c;

INSERT INTO curso_docentes (curso_id, docente_id, rol)
SELECT c.id, d.docente, d.rol
FROM (VALUES
    ('CUR-251', 'docente-2001', 'TITULAR'),
    ('CUR-252', 'docente-2002', 'TITULAR'),
    ('CUR-261', 'docente-2001', 'TITULAR'),
    ('CUR-261', 'docente-2004', 'AUXILIAR'),
    ('CUR-262', 'docente-2002', 'TITULAR'),
    ('CUR-263', 'docente-2003', 'TITULAR'),
    ('CUR-271', 'docente-2001', 'TITULAR'),
    ('CUR-271', 'docente-2004', 'AUXILIAR'),
    ('CUR-272', 'docente-2002', 'TITULAR'),
    ('CUR-273', 'docente-2005', 'TITULAR'),
    ('CUR-273', 'docente-2004', 'ADJUNTO'),
    ('CUR-274', 'docente-2003', 'TITULAR'),
    ('CUR-281', 'docente-2001', 'TITULAR'),
    ('CUR-283', 'docente-2003', 'TITULAR')
) AS d(curso, docente, rol)
JOIN cursos c ON c.codigo = d.curso;

INSERT INTO inscripciones_curso (curso_id, alumno_id, fecha_inscripcion, estado, nota_final, fecha_resultado)
SELECT c.id, d.alumno, d.fecha::DATE, d.estado, d.nota::NUMERIC, d.resultado::DATE
FROM (VALUES
    ('CUR-251', 'alumno-1007', '2025-07-21', 'APROBADO', '7.50', '2025-12-10'),
    ('CUR-252', 'alumno-1007', '2025-07-21', 'DESAPROBADO', '2.00', '2025-12-11'),

    ('CUR-261', 'alumno-1001', '2026-02-20', 'APROBADO', '8.50', '2026-07-07'),
    ('CUR-261', 'alumno-1002', '2026-02-20', 'APROBADO', '7.00', '2026-07-07'),
    ('CUR-261', 'alumno-1003', '2026-02-21', 'REGULAR', NULL, NULL),
    ('CUR-261', 'alumno-1004', '2026-02-21', 'DESAPROBADO', '3.00', '2026-07-07'),
    ('CUR-261', 'alumno-1005', '2026-02-22', 'LIBRE', NULL, NULL),
    ('CUR-261', 'alumno-1006', '2026-02-22', 'BAJA', NULL, NULL),
    ('CUR-262', 'alumno-1001', '2026-02-20', 'APROBADO', '9.00', '2026-07-08'),
    ('CUR-262', 'alumno-1002', '2026-02-20', 'REGULAR', NULL, NULL),
    ('CUR-262', 'alumno-1003', '2026-02-21', 'APROBADO', '6.00', '2026-07-08'),
    ('CUR-263', 'alumno-1008', '2026-02-23', 'APROBADO', '8.00', '2026-07-09'),
    ('CUR-263', 'alumno-1009', '2026-02-23', 'REGULAR', NULL, NULL),

    ('CUR-271', 'alumno-1001', '2026-07-20', 'CURSANDO', NULL, NULL),
    ('CUR-271', 'alumno-1002', '2026-07-20', 'CURSANDO', NULL, NULL),
    ('CUR-271', 'alumno-1003', '2026-07-21', 'CURSANDO', NULL, NULL),
    ('CUR-272', 'alumno-1001', '2026-07-20', 'CURSANDO', NULL, NULL),
    ('CUR-272', 'alumno-1003', '2026-07-21', 'CURSANDO', NULL, NULL),
    ('CUR-274', 'alumno-1008', '2026-07-22', 'CURSANDO', NULL, NULL),
    ('CUR-274', 'alumno-1009', '2026-07-22', 'CURSANDO', NULL, NULL),

    ('CUR-281', 'alumno-1004', '2026-10-28', 'INSCRIPTO', NULL, NULL),
    ('CUR-281', 'alumno-1010', '2026-10-28', 'INSCRIPTO', NULL, NULL),
    ('CUR-283', 'alumno-1011', '2026-10-29', 'INSCRIPTO', NULL, NULL)
) AS d(curso, alumno, fecha, estado, nota, resultado)
JOIN cursos c ON c.codigo = d.curso;

INSERT INTO asignaciones_aula (aula_id, asignatura_id, fecha, hora_inicio, hora_fin, cantidad_estudiantes)
SELECT au.id, a.id, d.fecha::DATE, d.inicio::TIME, d.fin::TIME, d.estudiantes
FROM (VALUES
    ('Sede Monserrat', 'A-102', 'INF-126', 'INF-101', '2026-11-09', '09:00', '11:00', 20),
    ('Sede Monserrat', 'LAB-1', 'INF-126', 'INF-105', '2026-11-11', '15:00', '17:00', 20),
    ('Sede Monserrat', 'A-101', 'INF-126', 'INF-104', '2026-11-20', '15:00', '17:00', 25),
    ('Sede Recoleta', 'R-201', 'ADM-126', 'ADM-101', '2026-11-13', '19:00', '21:00', 45),
    ('Sede Pilar', 'P-301', 'ADM-126', 'ADM-102', '2026-11-14', '10:00', '12:00', 40)
) AS d(sede, aula, plan, asignatura, fecha, inicio, fin, estudiantes)
JOIN sedes s ON s.nombre = d.sede
JOIN aulas au ON au.sede_id = s.id AND au.codigo = d.aula
JOIN planes_estudio p ON p.codigo = d.plan
JOIN asignaturas a ON a.plan_id = p.id AND a.codigo = d.asignatura;

COMMIT;

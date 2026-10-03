# Información de integración para módulos de Alumnos y Docencia

## Alcance

Este documento describe los endpoints y modelos que existen actualmente en Gestión Académica y que pueden servir de referencia a los módulos de Alumnos y Docencia.

Las rutas siguientes son las que implementa directamente este servicio. El gateway institucional puede publicar un prefijo distinto.

El Portal de Docencia consume estas rutas con `ROLE_ADMINISTRATIVO` (o `ROLE_ACADEMIC_ADMIN`) y puede luego presentar la información al alumno. No se exponen endpoints públicos ni de autoservicio directamente desde este servicio.

## Situación de los requerimientos del Portal de Docencia

| Requerimiento | Estado actual | Uso o extensión recomendada |
| --- | --- | --- |
| Mostrar las materias/cursos de un alumno | No existe una consulta por alumno. El detalle de un curso incluye sus inscripciones, pero no debe usarse para componer esta vista. | Incorporar `GET /api/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}`. Debe devolver cada inscripción junto con el curso y la asignatura. |
| Mostrar correlatividades | Implementado. | Usar `GET /api/academica/asignaturas/{asignaturaId}/correlatividades`. La regla también se valida al inscribir al alumno. |
| Determinar si una asignatura pertenece a un plan/carrera | No existe una consulta puntual. | Obtener los planes de la carrera y luego las asignaturas del plan; o incorporar un filtro `planId` al listado de cursos para evitar el filtrado del lado del Portal. |
| Mostrar el nombre de la asignatura en el listado de cursos | El listado devuelve solamente `asignaturaId`. El detalle de un curso ya trae el nombre. | Ampliar la respuesta de `GET /api/planificacion/cursos` con `asignatura: { id, codigo, nombre }`; es preferible a hacer un detalle por cada curso. |

Las extensiones indicadas en esta tabla son propuestas y **no están implementadas actualmente**.

## Cursos

### Listar cursos

`GET /api/planificacion/cursos?periodoId={id}`

El filtro `periodoId` es opcional. La respuesta es una lista con el DTO actual:

```json
[
  {
    "id": 42,
    "codigo": "DDA2-A",
    "asignaturaId": 15,
    "periodoId": 8,
    "sedeId": 2,
    "modalidad": "PRESENCIAL",
    "estado": "EN_CURSO",
    "cupoMaximo": 45,
    "fechaInicio": "2026-08-10",
    "fechaFin": "2026-11-30",
    "inscriptosActivos": 30
  }
]
```

Actualmente no admite `planId` y tampoco incluye el nombre de la asignatura. Para obtener cursos de un plan con el contrato actual, el Portal debe obtener las asignaturas de ese plan y filtrar por `asignaturaId`.

### Consultar detalle de curso

`GET /api/planificacion/cursos/{cursoId}`

El curso incluye ahora los datos básicos de asignatura y período de forma anidada. El detalle también contiene las asignaciones docentes, inscripciones y horarios:

```json
{
  "curso": {
    "id": 42,
    "codigo": "DDA2-A",
    "asignatura": {
      "id": 15,
      "codigo": "DDA2",
      "nombre": "Desarrollo de Aplicaciones II"
    },
    "periodo": {
      "id": 8,
      "anio": 2026,
      "numero": 2,
      "fechaInicio": "2026-08-01",
      "fechaFin": "2026-11-30",
      "inscripcionDesde": "2026-07-01",
      "inscripcionHasta": "2026-07-31"
    },
    "sedeId": 2,
    "modalidad": "PRESENCIAL",
    "estado": "EN_CURSO",
    "cupoMaximo": 45,
    "fechaInicio": "2026-08-10",
    "fechaFin": "2026-11-30",
    "inscriptosActivos": 30
  },
  "docentes": [
    { "id": 3, "docenteId": "docente-123", "rol": "TITULAR" }
  ],
  "inscripciones": [
    {
      "id": 71,
      "alumnoId": "alumno-456",
      "fechaInscripcion": "2026-07-15",
      "estado": "CURSANDO",
      "notaFinal": null,
      "fechaResultado": null
    }
  ],
  "horarios": [
    {
      "id": 9,
      "aulaId": 4,
      "diaSemana": "MONDAY",
      "horaInicio": "18:00:00",
      "horaFin": "20:00:00"
    }
  ]
}
```

El DTO de detalle refleja los datos que devuelve el endpoint. Los identificadores de alumno y docente son referencias a los módulos propietarios de esas identidades; este servicio no replica sus datos personales.

### Crear un curso

`POST /api/planificacion/cursos`

```json
{
  "codigo": "DDA2-A",
  "asignaturaId": 15,
  "periodoId": 8,
  "sedeId": 2,
  "modalidad": "PRESENCIAL",
  "cupoMaximo": 45,
  "fechaInicio": "2026-08-10",
  "fechaFin": "2026-11-30"
}
```

## Inscripciones y resultados

### Inscribir alumno

`POST /api/planificacion/cursos/{cursoId}/inscripciones`

El endpoint existente recibe el identificador del alumno en el cuerpo. Responde con `201 Created` y el DTO de inscripción.

```json
{
  "alumnoId": "alumno-456"
}
```

### Actualizar situación o resultado de una inscripción

`PUT /api/planificacion/cursos/{cursoId}/inscripciones/{alumnoId}`

El formato existente usa `notaFinal` y `fechaResultado`:

```json
{
  "estado": "APROBADO",
  "notaFinal": 8.0,
  "fechaResultado": "2026-09-30"
}
```

Respuesta:

```json
{
  "id": 71,
  "alumnoId": "alumno-456",
  "fechaInscripcion": "2026-07-15",
  "estado": "APROBADO",
  "notaFinal": 8.0,
  "fechaResultado": "2026-09-30"
}
```

Los valores admitidos para `estado` son `INSCRIPTO`, `CURSANDO`, `BAJA`, `REGULAR`, `LIBRE`, `APROBADO` y `DESAPROBADO`. `notaFinal` puede ser nula o estar entre 0 y 10.

No existe actualmente un `GET` que liste las inscripciones de un `alumnoId`. El Portal no debe consultar el detalle de todos los cursos para reconstruir esa información.

## Correlatividades

### Consultar correlatividades de una asignatura

`GET /api/academica/asignaturas/{asignaturaId}/correlatividades`

Devuelve las asignaturas que deben estar regularizadas o aprobadas para cursar la asignatura indicada:

```json
[
  {
    "id": 5,
    "asignaturaId": 15,
    "correlativaId": 12,
    "tipo": "APROBADA"
  },
  {
    "id": 6,
    "asignaturaId": 15,
    "correlativaId": 13,
    "tipo": "REGULAR"
  }
]
```

`APROBADA` requiere que la asignatura correlativa esté aprobada. `REGULAR` admite que esté regular o aprobada. El endpoint requiere `ROLE_ADMINISTRATIVO` o `ROLE_ACADEMIC_ADMIN`, por lo que el Portal de Docencia puede consumirlo con sus credenciales actuales. La misma validación se ejecuta al crear una inscripción.

## Calendario: turnos de examen

### Listar períodos académicos

`GET /api/planificacion/periodos`

```json
[
  {
    "id": 8,
    "anio": 2026,
    "numero": 2,
    "fechaInicio": "2026-08-01",
    "fechaFin": "2026-11-30",
    "inscripcionDesde": "2026-07-01",
    "inscripcionHasta": "2026-07-31"
  }
]
```

### Listar turnos

`GET /api/planificacion/turnos-examen`

La respuesta actual contiene `nombre` (no `titulo`) y las fechas del turno y de su período de inscripción:

```json
[
  {
    "id": 7,
    "nombre": "Turno final diciembre",
    "fechaInicio": "2026-12-01",
    "fechaFin": "2026-12-15",
    "inscripcionDesde": "2026-11-10",
    "inscripcionHasta": "2026-11-25"
  }
]
```

## Evento de notificación de resultado

Al publicar un resultado final (`APROBADO` o `DESAPROBADO` con `fechaResultado`), el servicio construye este evento. Si `ANALYTICS_QUEUE_ENABLED=true`, lo publica como JSON en RabbitMQ después de confirmar la transacción. Los reintentos del mismo alumno y curso conservan `eventId` para que Analítica pueda deduplicarlos.

```json
{
  "eventId": "acad-res-alumno-456-curso-42",
  "sourceModule": "academica",
  "eventType": "resultado.publicado",
  "occurredAt": "2026-09-30T14:30:00Z",
  "payload": {
    "alumnoId": "alumno-456",
    "cursoId": 42,
    "materia": "BDD-310",
    "comision": "B1",
    "docente": "docente-12",
    "sede": "Sede Montserrat",
    "cuatrimestre": "2026-2Q",
    "notaFinal": 8.0,
    "estado": "APROBADO",
    "aprobado": true,
    "fechaResultado": "2026-09-30"
  }
}
```

El exchange, cola y routing key se configuran con `ANALYTICS_QUEUE_EXCHANGE`,
`ANALYTICS_QUEUE_NAME` y `ANALYTICS_QUEUE_ROUTING_KEY`, respectivamente. Por defecto:
`academica.eventos`, `analitica.resultados` y `resultado.publicado`.

## Entidades y referencias para desarrollo local

Los módulos pueden copiar estos modelos mínimos para desarrollar con datos locales. Los IDs de `asignaturaId`, `periodoId`, `sedeId`, `cursoId`, `alumnoId` y `docenteId` son referencias de integración. En producción, cada módulo debe tratar como fuente de verdad los datos del servicio propietario.

| Entidad | Campos del modelo actual relevantes para integración |
| --- | --- |
| `Curso` | `id: Long`, `codigo: String`, `asignaturaId: Long`, `periodoId: Long`, `sedeId: Long`, `modalidad: ModalidadCurso`, `estado: EstadoCurso`, `cupoMaximo: int`, `fechaInicio: LocalDate`, `fechaFin: LocalDate` |
| `InscripcionCurso` | `id: Long`, `cursoId: Long`, `alumnoId: String`, `fechaInscripcion: LocalDate`, `estado: EstadoInscripcionCurso`, `notaFinal: BigDecimal?`, `fechaResultado: LocalDate?` |
| `Asignatura` | `id: Long`, `codigo: String`, `nombre: String`, `anio: String`, `creditos: int`, `cargaHoraria: int`, `estado: EstadoAcademico`, `planId: Long` |
| `Correlatividad` | `id: Long`, `asignaturaId: Long`, `correlativaId: Long`, `tipo: Correlatividad.Tipo` |
| `PeriodoAcademico` | `id: Long`, `anio: int`, `numero: int`, `fechaInicio: LocalDate`, `fechaFin: LocalDate`, `inscripcionDesde: LocalDate`, `inscripcionHasta: LocalDate` |
| `TurnoExamen` | `id: Long`, `nombre: String`, `fechaInicio: LocalDate`, `fechaFin: LocalDate`, `inscripcionDesde: LocalDate`, `inscripcionHasta: LocalDate` |
| `CursoDocente` | `id: Long`, `cursoId: Long`, `docenteId: String`, `rol: RolDocenteCurso` |
| `HorarioCurso` | `id: Long`, `cursoId: Long`, `aulaId: Long`, `diaSemana: DayOfWeek`, `horaInicio: LocalTime`, `horaFin: LocalTime` |
| `Sede` | `id: Long`, `backofficeSiteId: UUID?`, `nombre: String`, `direccion: String`, `estado: String` |
| `Aula` | `id: Long`, `backofficeLocationId: UUID?`, `codigo: String`, `nombre: String`, `tipo: TipoUbicacion`, `capacidadMaxima: int`, `sedeId: Long`, `estado: String` |

Enumeraciones usadas por estos modelos:

- `EstadoCurso`: `PLANIFICADO`, `EN_CURSO`, `FINALIZADO`, `CANCELADO`.
- `EstadoInscripcionCurso`: `INSCRIPTO`, `CURSANDO`, `BAJA`, `REGULAR`, `LIBRE`, `APROBADO`, `DESAPROBADO`.
- `RolDocenteCurso`: `TITULAR`, `ADJUNTO`, `AUXILIAR`.
- `Correlatividad.Tipo`: `REGULAR`, `APROBADA`.
- `ModalidadCurso` y `EstadoAcademico`: replicar los valores del servicio antes de intercambiar esos campos.

Para mantener los ejemplos en sincronía, los campos de respuesta reflejan los DTOs del backend. El modelo local de otro módulo puede ser más pequeño, pero debe conservar el significado y los identificadores compartidos.

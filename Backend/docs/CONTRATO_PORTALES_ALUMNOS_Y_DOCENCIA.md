# Información de integración para módulos de Alumnos y Docencia

## Alcance

Este documento describe los endpoints y modelos que existen actualmente en Gestión Académica y que pueden servir de referencia a los módulos de Alumnos y Docencia.

Las rutas siguientes son las que implementa directamente este servicio. El gateway institucional puede publicar un prefijo distinto.

El Portal de Docencia consume estas rutas con `ROLE_ADMINISTRATIVO` (o `ROLE_ACADEMIC_ADMIN`) y puede luego presentar la información al alumno. No se exponen endpoints públicos ni de autoservicio directamente desde este servicio.

## Situación de los requerimientos del Portal de Docencia

| Requerimiento | Estado actual | Uso o extensión recomendada |
| --- | --- | --- |
| Mostrar las materias/cursos de un alumno | Implementado. | Usar `GET /api/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}`. Devuelve cada inscripción junto con el curso, la asignatura y el período. |
| Mostrar correlatividades | Implementado. | Usar `GET /api/academica/asignaturas/{asignaturaId}/correlatividades`. La regla también se valida al inscribir al alumno. |
| Determinar si una asignatura pertenece a un plan/carrera | No existe una consulta puntual. | Usar `GET /api/academica/carreras/{carreraId}/planes` y luego `GET /api/academica/planes/{planId}/asignaturas`. Si no se conoce el ID de la carrera, obtenerlo con `GET /api/academica/carreras`. Para filtrar cursos de un plan, comparar los `asignaturaId` del listado de cursos con los IDs de esas asignaturas. |
| Mostrar el nombre de la asignatura en el listado de cursos | El listado devuelve solamente `asignaturaId`. El detalle de un curso ya trae el nombre. | Ampliar la respuesta de `GET /api/planificacion/cursos` con `asignatura: { id, codigo, nombre }`; es preferible a hacer un detalle por cada curso. |

Las filas marcadas como pendientes describen propuestas que todavía no están implementadas.

## Carreras, planes y asignaturas

Estas consultas ya existen en Gestión Académica:

| Método | Ruta | Devuelve |
| --- | --- | --- |
| `GET` | `/api/academica/carreras` | Carreras con `id`, `codigo`, `nombre`, `facultad`, `duracion`, `titulo` y `estado`. Sirve para encontrar el `carreraId`. |
| `GET` | `/api/academica/carreras/{carreraId}/planes` | Planes de esa carrera con `id`, `codigo`, `nombre`, `vigenciaDesde`, `cantidadAsignaturas`, `estado` y `carreraId`. |
| `GET` | `/api/academica/planes/{planId}/asignaturas` | Asignaturas de ese plan con `id`, `codigo`, `nombre`, `anio`, `creditos`, `cargaHoraria`, `estado` y `planId`. |

Para comprobar si una asignatura pertenece a una carrera, consultar los planes de la
carrera y buscar su `id` en las asignaturas de cada plan. Si ya se conoce el `planId`,
alcanza con la última consulta. `GET /api/planificacion/cursos` devuelve `asignaturaId`,
que puede compararse con esos IDs para identificar los cursos correspondientes.
Actualmente no hay una consulta directa por par `carreraId`/`asignaturaId` ni un
filtro `planId` en el listado de cursos. Estas rutas usan los permisos de lectura
académica indicados en el alcance de este contrato.

## Cursos

Los nombres de sede y aula se consultan desde sus tablas relacionadas. No se duplican
en `cursos`; las respuestas muestran el nombre vigente junto a `sedeId` y `aulaId`.

### Listar cursos

`GET /api/planificacion/cursos?periodoId={id}`

El filtro `periodoId` es opcional. La respuesta incluye `sedeNombre` y `aulaNombre` junto a sus IDs. `aulaNombre` puede ser `null` si no hay aula asociada:

```json
[
  {
    "id": 42,
    "codigo": "DDA2-A",
    "asignaturaId": 15,
    "periodoId": 8,
    "sedeId": 2,
    "sedeNombre": "Sede Centro",
    "modalidad": "PRESENCIAL",
    "turno": "TARDE",
    "horaInicio": "14:00:00",
    "horaFin": "18:00:00",
    "diaSemana": "MONDAY",
    "aulaId": 4,
    "aulaNombre": "Laboratorio 4",
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

El curso incluye datos de asignatura y período de forma anidada, y los nombres de sede y aula. El detalle también contiene las asignaciones docentes e inscripciones:

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
    "sedeNombre": "Sede Centro",
    "modalidad": "PRESENCIAL",
    "turno": "TARDE",
    "horaInicio": "14:00:00",
    "horaFin": "18:00:00",
    "diaSemana": "MONDAY",
    "aulaId": 4,
    "aulaNombre": "Laboratorio 4",
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
  "turno": "TARDE",
  "horaInicio": "14:00",
  "horaFin": "18:00",
  "diaSemana": "MONDAY",
  "aulaId": 4,
  "cupoMaximo": 45,
  "fechaInicio": "2026-08-10",
  "fechaFin": "2026-11-30"
}
```

### Actualizar el único horario del curso

`PUT /api/planificacion/cursos/{cursoId}/horario`

Recibe `aulaId`, `diaSemana`, `horaInicio` y `horaFin`. Reemplaza esos campos en `Curso` y devuelve el curso actualizado.

## Inscripciones y resultados

### Listar inscripciones de un alumno

`GET /api/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}`

El filtro `periodoId` es opcional. La respuesta se ordena por período descendente y, dentro del
período, por código de curso. Cada elemento incluye el estado y resultado de la inscripción, y el
curso con su asignatura y período. Este es el endpoint que debe usar el Portal de Alumnos para
mostrar las materias de un alumno.

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

### Notas finales para el portal docente

La nota pertenece a `InscripcionCurso`: cada inscripción ya identifica un `cursoId` y un
`alumnoId` único y guarda `notaFinal`, `estado` y `fechaResultado`. Se usa el nombre
compartido entre módulos, `alumnoId`, para el identificador del alumno; se conserva
como `String` y no se reemplaza por el ID local de la inscripción.

`GET /api/planificacion/cursos/{cursoId}/notas` lista los alumnos inscriptos en el curso,
incluidos los que todavía tienen `notaFinal: null`. Responde con una lista de objetos
`{ alumnoId, notaFinal, estado, fechaResultado }` ordenada por `alumnoId`.

`GET /api/planificacion/cursos/{cursoId}/notas/{alumnoId}` consulta un alumno inscripto.

`PUT /api/planificacion/cursos/{cursoId}/notas/{alumnoId}` carga o corrige su nota final:

```json
{
  "notaFinal": 8.5,
  "estado": "APROBADO",
  "fechaResultado": "2026-10-12"
}
```

La nota debe estar entre 0 y 10, y el estado debe ser `APROBADO` o `DESAPROBADO`.
Si se omite `fechaResultado`, se usa la fecha actual del servidor. La inscripción debe
existir y no estar dada de baja. Una modificación publica el evento
`resultado.publicado`; repetir la misma petición sin cambios no lo publica otra vez.

Actualmente la seguridad del backend permite estas rutas a roles administrativos.
La autorización del portal docente se define por separado del contrato de nombres
y datos de esta API.

Para listar las inscripciones de un alumno se usa `GET /api/planificacion/alumnos/{alumnoId}/inscripciones`; cada elemento incluye el mismo resumen de curso con sede y aula.

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
| `Curso` | `id: Long`, `codigo: String`, `asignaturaId: Long`, `periodoId: Long`, `sedeId: Long`, `modalidad: ModalidadCurso`, `estado: EstadoCurso`, `cupoMaximo: int`, `fechaInicio: LocalDate`, `fechaFin: LocalDate`, `turno: TurnoCurso`, `horaInicio: LocalTime`, `horaFin: LocalTime`, `diaSemana: DayOfWeek`, `aulaId: Long?`; las respuestas incluyen `sedeNombre: String` y `aulaNombre: String?` |
| `InscripcionCurso` | `id: Long`, `cursoId: Long`, `alumnoId: String`, `fechaInscripcion: LocalDate`, `estado: EstadoInscripcionCurso`, `notaFinal: BigDecimal?`, `fechaResultado: LocalDate?` |
| `Asignatura` | `id: Long`, `codigo: String`, `nombre: String`, `anio: int`, `creditos: int`, `cargaHoraria: int`, `estado: EstadoAcademico`, `planId: Long` |
| `Correlatividad` | `id: Long`, `asignaturaId: Long`, `correlativaId: Long`, `tipo: Correlatividad.Tipo` |
| `PeriodoAcademico` | `id: Long`, `anio: int`, `numero: int`, `fechaInicio: LocalDate`, `fechaFin: LocalDate`, `inscripcionDesde: LocalDate`, `inscripcionHasta: LocalDate` |
| `TurnoExamen` | `id: Long`, `nombre: String`, `fechaInicio: LocalDate`, `fechaFin: LocalDate`, `inscripcionDesde: LocalDate`, `inscripcionHasta: LocalDate` |
| `CursoDocente` | `id: Long`, `cursoId: Long`, `docenteId: String`, `rol: RolDocenteCurso` |
| `Sede` | `id: Long`, `backofficeSiteId: UUID?`, `nombre: String`, `direccion: String`, `estado: String` |
| `Aula` | `id: Long`, `backofficeLocationId: UUID?`, `codigo: String`, `nombre: String`, `tipo: TipoUbicacion`, `capacidadMaxima: int`, `sedeId: Long`, `estado: String` |

Enumeraciones usadas por estos modelos:

- `EstadoCurso`: `PLANIFICADO`, `EN_CURSO`, `FINALIZADO`, `CANCELADO`.
- `TurnoCurso`: `TARDE`, `MAÑANA`, `NOCHE`. En `POST /api/planificacion/cursos`, los tres campos son obligatorios y `horaFin` debe ser posterior a `horaInicio`. Los cursos anteriores a esta ampliación pueden devolver `null` en esos campos hasta completar sus datos.
- `EstadoInscripcionCurso`: `INSCRIPTO`, `CURSANDO`, `BAJA`, `REGULAR`, `LIBRE`, `APROBADO`, `DESAPROBADO`.
- `RolDocenteCurso`: `TITULAR`, `ADJUNTO`, `AUXILIAR`.
- `Correlatividad.Tipo`: `REGULAR`, `APROBADA`.
- `ModalidadCurso` y `EstadoAcademico`: replicar los valores del servicio antes de intercambiar esos campos.

Para mantener los ejemplos en sincronía, los campos de respuesta reflejan los DTOs del backend. El modelo local de otro módulo puede ser más pequeño, pero debe conservar el significado y los identificadores compartidos.

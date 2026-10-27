# Información de integración para módulos de Alumnos y Docencia

## Alcance

Este documento describe los endpoints y modelos que existen actualmente en Gestión Académica y que pueden servir de referencia a los módulos de Alumnos y Docencia.

Las rutas siguientes son las que implementa directamente este servicio. El gateway institucional puede publicar un prefijo distinto.

El Portal de Docencia consume estas rutas con `ROLE_ADMINISTRATIVO` o `ROLE_ACADEMIC_ADMIN` y puede luego presentar la información al alumno. No se exponen endpoints públicos ni de autoservicio directamente desde este servicio.

## Estado real de la integración

Este contrato distingue deliberadamente entre la API que funciona hoy y la integración
objetivo con CORE:

| Capa | Estado | Contrato vigente |
| --- | --- | --- |
| API local de Gestión Académica | Implementada | Rutas `/api/**`, autenticación local por sesión `JSESSIONID` y roles `ROLE_ADMINISTRATIVO`/`ROLE_ACADEMIC_ADMIN`. |
| Gateway de CORE | Pendiente | CORE publicará el módulo bajo `/api/v1/academic/*` y reescribirá a `/api/v1/*`. Los controladores todavía no exponen ese prefijo. |
| JWT de CORE | Pendiente | Falta validar RS256, `iss`, `aud` y `exp`, y mapear `roles`/`permissions`. La configuración OpenAPI ya anuncia Bearer, pero la seguridad de ejecución todavía no lo procesa. |
| Headers `X-User-*` | Pendiente | Falta consumir `X-User-Id`, `X-User-Roles`, `X-User-Permissions` y propagar `X-Correlation-Id`. |
| Endpoints privados `/internal/v1/**` | No implementados | El módulo actualmente no expone endpoints backend-a-backend. |
| Backoffice | Preparado, no conectado | Existen referencias `backofficeSiteId`/`backofficeLocationId`; falta el cliente HTTP y la sincronización paginada. |
| Eventos CORE | Pendiente de adaptar | El publicador Rabbit opcional actual usa un envelope y topología propios. CORE exige publicar mediante su contrato, no declarar exchanges/colas desde este módulo. |
| Errores CORE | Parcial | Los errores de aplicación usan `application/problem+json` con `code` y `correlationId`; falta aplicar la misma forma a los `401` y `403` de Spring Security. |

Por lo tanto, las rutas listadas abajo son rutas internas actuales del servicio. No deben
exponerse directamente a Internet. La URL pública definitiva debe pasar por el gateway.

## Inventario completo de endpoints actuales

### Sin autenticación

| Método | Ruta | Resultado |
| --- | --- | --- |
| `GET` | `/health/live` | Liveness del proceso. |
| `GET` | `/health/ready` | Readiness del servicio. Comprueba la conexión a PostgreSQL y devuelve `503 { "status": "DOWN" }` si no está disponible. |
| `POST` | `/api/auth/login` | Autentica email/contraseña local, crea sesión y devuelve el usuario sin contraseña. |
| `GET` | `/swagger-ui.html` | Interfaz Swagger. |
| `GET` | `/v3/api-docs` | Documento OpenAPI generado. |

### Autenticación local

| Método | Ruta | Actividad |
| --- | --- | --- |
| `GET` | `/api/auth/me` | Devuelve el usuario de la sesión actual. |
| `POST` | `/api/auth/logout` | Invalida la sesión actual. |

No existen controladores `/api/usuarios`, ni endpoints para crear o listar usuarios.
`UsuarioServicio` contiene esos métodos, pero no están expuestos por HTTP.

### Estructura académica

| Método | Ruta | Actividad |
| --- | --- | --- |
| `POST` | `/api/academica/carreras` | Crear carrera. |
| `GET` | `/api/academica/carreras` | Listar carreras. |
| `PUT` | `/api/academica/carreras/{id}` | Actualizar carrera. |
| `DELETE` | `/api/academica/carreras/{id}` | Desactivación lógica: cambia el estado a `INACTIVA`. |
| `POST` | `/api/academica/carreras/{carreraId}/planes` | Crear plan de estudio. |
| `GET` | `/api/academica/carreras/{carreraId}/planes` | Listar planes de una carrera. |
| `GET` | `/api/academica/planes/{planId}/pdf` | Descargar PDF con asignaturas y correlatividades del plan. |
| `POST` | `/api/academica/planes/{planId}/asignaturas` | Crear asignatura en un plan. |
| `GET` | `/api/academica/planes/{planId}/asignaturas` | Listar asignaturas de un plan. |
| `POST` | `/api/academica/asignaturas/{asignaturaId}/correlatividades` | Agregar correlatividad. |
| `GET` | `/api/academica/asignaturas/{asignaturaId}/correlatividades` | Listar correlatividades. |

### Espacios y calendario académico

| Método | Ruta | Actividad |
| --- | --- | --- |
| `POST` | `/api/planificacion/sedes` | Crear sede local. |
| `GET` | `/api/planificacion/sedes` | Listar sedes. |
| `POST` | `/api/planificacion/sedes/{sedeId}/aulas` | Crear aula local asociada a una sede. |
| `GET` | `/api/planificacion/sedes/{sedeId}/aulas` | Listar aulas de la sede. |
| `POST` | `/api/planificacion/asignaciones` | Reservar un aula para una asignatura en una fecha y horario. |
| `GET` | `/api/planificacion/asignaciones` | Listar asignaciones registradas, ordenadas por fecha y hora. |
| `GET` | `/api/planificacion/aulas/{aulaId}/agenda?fecha=AAAA-MM-DD` | Consultar agenda diaria unificada del aula, incluyendo cursos y asignaciones puntuales. |
| `GET` | `/api/planificacion/aulas/{aulaId}/agenda?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Consultar cursos y asignaciones puntuales del aula entre dos fechas inclusivas, ordenados por fecha y hora. |
| `GET` | `/api/planificacion/asignaturas/{asignaturaId}/agenda?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Consultar horarios ocupados de la asignatura entre dos fechas inclusivas. |
| `POST` | `/api/planificacion/periodos` | Crear período académico y ventana de inscripción. |
| `GET` | `/api/planificacion/periodos` | Listar períodos. |
| `POST` | `/api/planificacion/turnos-examen` | Crear turno de examen y ventana de inscripción. |
| `GET` | `/api/planificacion/turnos-examen` | Listar turnos de examen. |

La agenda del aula devuelve una lista unificada con `tipo` (`CURSO` o `ASIGNACION`),
`referenciaId`, aula, asignatura, fecha y horario. Para cursos también informa
`cursoCodigo`; para asignaciones puntuales informa `cantidadEstudiantes`. Cada curso
se expande en una ocurrencia por semana dentro de sus fechas de inicio y fin.

### Cursos, docentes, inscripciones y notas

| Método | Ruta | Actividad |
| --- | --- | --- |
| `POST` | `/api/planificacion/cursos` | Crear curso/comisión. |
| `GET` | `/api/planificacion/cursos?periodoId={id}` | Listar cursos; filtro de período opcional. |
| `GET` | `/api/planificacion/cursos/{cursoId}` | Obtener curso con docentes e inscripciones. |
| `GET` | `/api/planificacion/cursos/{cursoId}/docentes` | Listar docentes asignados al curso. |
| `GET` | `/api/planificacion/cursos/{cursoId}/alumnos` | Listar alumnos inscriptos y su estado académico. |
| `PUT` | `/api/planificacion/cursos/{cursoId}/estado` | Cambiar estado del curso. |
| `PUT` | `/api/planificacion/cursos/{cursoId}/horario` | Cambiar día, aula y rango horario. |
| `POST` | `/api/planificacion/cursos/{cursoId}/docentes` | Asignar docente externo y rol. |
| `POST` | `/api/planificacion/cursos/{cursoId}/inscripciones` | Inscribir alumno. |
| `PUT` | `/api/planificacion/cursos/{cursoId}/inscripciones/{alumnoId}` | Actualizar estado o resultado de inscripción. |
| `GET` | `/api/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}` | Listar inscripciones de un alumno. |
| `GET` | `/api/planificacion/cursos/{cursoId}/notas` | Listar notas del curso. |
| `GET` | `/api/planificacion/cursos/{cursoId}/notas/{alumnoId}` | Consultar nota de un alumno. |
| `PUT` | `/api/planificacion/cursos/{cursoId}/notas/{alumnoId}` | Cargar o corregir nota final. |

### Regularidad

| Método | Ruta | Actividad |
| --- | --- | --- |
| `POST` | `/api/regularidad/validar` | Evalúa asistencia y promedio para un alumno y curso persistidos; exige inscripción activa y correlativas aprobadas para habilitar el final. |

## Catálogo de payloads de escritura

Los nombres siguientes son los campos JSON exactos aceptados hoy. Las fechas usan
`AAAA-MM-DD`, las horas `HH:mm:ss` (Spring también acepta `HH:mm`) y los identificadores
locales son números `Long`. `alumnoId` y `docenteId` son strings externos; en la
integración objetivo deberán transportar el UUID de CORE como texto.

| Actividad | Body JSON | Respuesta exitosa |
| --- | --- | --- |
| Login local | `{ "email": "...", "password": "..." }` | `200`, usuario autenticado y cookie `JSESSIONID`. |
| Crear/editar carrera | `{ "codigo", "nombre", "facultad", "titulo" }` | `201` al crear, `200` al editar; la respuesta incluye `duracion`, calculada automáticamente según las materias de la carrera. |
| Crear plan | `{ "codigo", "nombre", "vigenciaDesde", "cantidadAsignaturas" }` | `201`; plan con `id`, `estado` y `carreraId`. |
| Crear asignatura | `{ "codigo", "nombre", "anio", "creditos", "cargaHoraria" }` | `201`; asignatura con `id`, `estado` y `planId`. |
| Crear correlatividad | `{ "correlativaId": 1, "tipo": "REGULAR|APROBADA" }` | `201`; relación creada. |
| Crear sede | `{ "nombre", "direccion" }` | `201`; sede con `id`, `backofficeSiteId` y `estado`. |
| Crear aula | `{ "codigo", "capacidadMaxima", "nombre"? }` | `201`; aula tipo `AULA`; si falta `nombre`, usa el código. |
| Asignar aula | `{ "aulaId", "asignaturaId", "fecha", "horaInicio", "horaFin", "cantidadEstudiantes" }` | `201`; asignación persistida. |
| Crear período | `{ "anio", "numero", "fechaInicio", "fechaFin", "inscripcionDesde", "inscripcionHasta" }` | `201`; período creado. `numero` admite 1 o 2. |
| Crear turno de examen | `{ "nombre", "fechaInicio", "fechaFin", "inscripcionDesde", "inscripcionHasta" }` | `201`; turno creado. |
| Crear curso | `{ "codigo", "asignaturaId", "periodoId", "sedeId", "modalidad", "cupoMaximo", "fechaInicio", "fechaFin", "turno", "horaInicio", "horaFin", "diaSemana", "aulaId" }` | `201`; curso con nombres de sede/aula e `inscriptosActivos`. |
| Cambiar estado de curso | `{ "estado": "PLANIFICADO|EN_CURSO|FINALIZADO|CANCELADO" }` | `200`; curso actualizado. |
| Cambiar horario de curso | `{ "aulaId", "diaSemana", "horaInicio", "horaFin" }` | `200`; curso actualizado después de revalidar aula y solapamiento. |
| Asignar docente | `{ "docenteId", "rol": "TITULAR|ADJUNTO|AUXILIAR" }` | `201`; vínculo docente-curso. |
| Inscribir alumno | `{ "alumnoId" }` | `201`; inscripción en estado `INSCRIPTO`. |
| Cambiar inscripción | `{ "estado", "notaFinal"?, "fechaResultado"? }` | `200`; inscripción actualizada. Los campos opcionales pasan a ser obligatorios para un estado final. |
| Cargar nota final | `{ "notaFinal", "estado": "APROBADO|DESAPROBADO", "fechaResultado"? }` | `200`; nota publicada. Si falta fecha se usa la fecha actual. |
| Validar regularidad | `{ "alumnoId", "cursoId", "asistencia", "promedio", "asistenciaMinima", "promedioMinimo" }` | `200`; devuelve resultado y contexto del alumno, curso y asignatura. Requiere inscripción activa. |

Las consultas devuelven listas de los DTO de respuesta anteriores. El detalle de curso
devuelve `{ "curso", "docentes", "inscripciones" }`; el listado de inscripciones de un
alumno anida un resumen completo del curso. La descarga del plan responde
`application/pdf`; el resto de los endpoints de negocio usa JSON.

Salvo health, Swagger y login, la configuración actual exige
`ROLE_ADMINISTRATIVO` o `ROLE_ACADEMIC_ADMIN` para todas las rutas anteriores.

## Reglas de negocio consolidadas

- Carreras: el código debe ser único sin distinguir mayúsculas/minúsculas; eliminar es
  una desactivación lógica.
- Códigos nuevos de carrera, plan, asignatura y curso: formato `ARQ-001` o
  `ARQ1-001`.
- Correlatividades: una asignatura no puede ser correlativa de sí misma y ambas deben
  pertenecer al mismo plan. Una materia sólo puede configurarse una vez como correlativa
  de otra; repetirla con el mismo tipo o con un tipo distinto responde HTTP 400 con
  `BUSINESS_RULE_VIOLATION`.
- Aulas: sólo ubicaciones de tipo `AULA` pueden asignarse; la cantidad de estudiantes o
  el cupo del curso no puede superar su capacidad.
- Ocupación: no se permiten horarios superpuestos para la misma aula, tampoco entre
  tipos distintos. Las asignaciones puntuales comparan fecha y hora; los cursos comparan
  día de semana y período, y la agenda expande cada ocurrencia semanal dentro del rango.
- Cursos: la fecha final debe ser posterior a la inicial y ambas deben caer dentro del
  período académico; la hora final debe ser posterior a la inicial; el aula debe
  pertenecer a la sede elegida.
- Inscripciones: no se aceptan en cursos cancelados, fuera de la ventana del período,
  duplicadas, por encima del cupo ni sin cumplir correlatividades.
- Correlatividad `REGULAR`: admite antecedente `REGULAR` o `APROBADO`.
  Correlatividad `APROBADA`: exige antecedente `APROBADO`.
- Resultados: una nota está entre 0 y 10. Los estados finales son `APROBADO` o
  `DESAPROBADO`; un resultado final necesita nota y fecha. No se cargan notas a una
  inscripción en `BAJA`.
- Períodos y turnos: todo rango termina después de comenzar. La inscripción debe cerrar
  antes o el mismo día del inicio del período, y antes del inicio del turno de examen.
- Regularidad: queda regular y habilitado para final únicamente cuando asistencia y
  promedio alcanzan los mínimos recibidos.

Enums expuestos: `EstadoAcademico = ACTIVA | BORRADOR | INACTIVA`,
`EstadoCurso = PLANIFICADO | EN_CURSO | FINALIZADO | CANCELADO`,
`EstadoInscripcionCurso = INSCRIPTO | CURSANDO | BAJA | REGULAR | LIBRE | APROBADO | DESAPROBADO`,
`ModalidadCurso = PRESENCIAL | VIRTUAL | HIBRIDA`,
`TurnoCurso = MAÑANA | TARDE | NOCHE`, `RolDocenteCurso = TITULAR | ADJUNTO | AUXILIAR`
y `TipoUbicacion = AULA | AUDITORIO | COMEDOR | BIBLIOTECA`.

## Errores actuales

Los errores de aplicación responden `application/problem+json` con `type`, `title`,
`status`, `detail`, `instance`, `code` y `correlationId`. Los errores de validación
agregan `errors`, un objeto por campo.

| Caso | HTTP | `code` |
| --- | --- | --- |
| Recurso inexistente | `404` | `RESOURCE_NOT_FOUND` |
| Regla de negocio | `400` | `BUSINESS_RULE_VIOLATION` |
| Validación de DTO | `400` | `VALIDATION_ERROR` |
| Error de persistencia | `500` | `DATA_ACCESS_ERROR` |
| Sin sesión/token | `401` | Respuesta de Spring Security pendiente de unificar. |
| Sin rol requerido | `403` | Respuesta de Spring Security pendiente de unificar. |

## Formato obligatorio de códigos

Los códigos nuevos de carreras, planes de estudio, asignaturas y cursos deben cumplir
la expresión regular `^[A-Z]{3}[0-9]?-[0-9]{3}$`. Se aceptan exactamente estos dos
formatos:

- Tres letras mayúsculas, guion y tres dígitos: `ARQ-001`.
- Tres letras mayúsculas, un dígito, guion y tres dígitos: `ARQ1-001`.

Los `POST` correspondientes y la actualización de carreras responden HTTP 400 cuando
el código no respeta el formato. Los módulos consumidores deben usar el `id` como
referencia entre entidades; el código es el identificador legible que debe mostrarse
junto al nombre en selectores y listados.

### Resolución de relaciones para formularios

Para crear cursos y asignaciones, los clientes deben obtener los registros reales y
mostrar opciones legibles, conservando el `id` como valor enviado:

- Asignaturas: recorrer `GET /api/academica/carreras`, luego los planes de cada carrera
  y finalmente `GET /api/academica/planes/{planId}/asignaturas`. Mostrar
  `codigo - nombre`; enviar `asignaturaId`.
- Períodos: usar `GET /api/planificacion/periodos`; enviar `periodoId`.
- Sedes: usar `GET /api/planificacion/sedes`; enviar `sedeId`.
- Aulas: consultar `GET /api/planificacion/sedes/{sedeId}/aulas`, mostrar
  `codigo - nombre` y enviar `aulaId`. En cursos, el aula debe pertenecer a la sede
  seleccionada.

`POST /api/planificacion/cursos` recibe `asignaturaId`, `periodoId`, `sedeId` y
`aulaId`. `POST /api/planificacion/asignaciones` recibe `asignaturaId` y `aulaId`.

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

La duración no se recibe al crear o editar una carrera. Se deriva de la cantidad de
asignaturas asociadas a sus planes: cinco materias completan un cuatrimestre y diez,
un año. Una fracción de cuatrimestre se redondea al cuatrimestre necesario.

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
    "codigo": "DDA2-001",
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
    "codigo": "DDA2-001",
    "asignatura": {
      "id": 15,
      "codigo": "DDA2-002",
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

### Consultar docentes y alumnos de un curso

La edición del curso puede consultar las dos colecciones sin ejecutar operaciones de alta o modificación:

- `GET /api/planificacion/cursos/{cursoId}/docentes` devuelve una lista de objetos
  `{ id, docenteId, rol }`, ordenada por el ID interno de la asignación.
- `GET /api/planificacion/cursos/{cursoId}/alumnos` devuelve una lista de objetos
  `{ id, alumnoId, fechaInscripcion, estado, notaFinal, fechaResultado }`, ordenada por
  `alumnoId`.

Ambas consultas responden `404` si el curso no existe. Las operaciones de escritura se
mantienen disponibles para los módulos responsables, pero la pantalla de edición del
frontend usa estas rutas sólo para mostrar docentes y alumnos.

### Crear un curso

`POST /api/planificacion/cursos`

```json
{
  "codigo": "DDA2-001",
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

Recibe el identificador en la ruta y devuelve un único objeto con las condiciones que
deben cumplir las materias correlativas. Las materias se agrupan por `tipo`:

```json
{
  "asignaturaId": 15,
  "condiciones": [
    {
      "tipo": "REGULAR",
      "materiasCorrelativas": [
        { "id": 13, "codigo": "MAT-013", "nombre": "Matemática II" }
      ]
    },
    {
      "tipo": "APROBADA",
      "materiasCorrelativas": [
        { "id": 11, "codigo": "ALG-011", "nombre": "Álgebra" },
        { "id": 12, "codigo": "PRG-012", "nombre": "Programación II" }
      ]
    }
  ]
}
```

Si la asignatura no tiene correlativas, `condiciones` es una lista vacía. Cada elemento
de `materiasCorrelativas` contiene el `id`, `codigo` y `nombre` vigentes de la asignatura
requerida, y la lista se ordena por `id`. `APROBADA` requiere que la asignatura
correlativa esté aprobada; `REGULAR` admite que esté regular o aprobada. El endpoint
requiere `ROLE_ADMINISTRATIVO` o `ROLE_ACADEMIC_ADMIN`, por lo que el Portal de
Docencia puede consumirlo con sus credenciales actuales. La misma validación se
ejecuta al crear una inscripción.

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

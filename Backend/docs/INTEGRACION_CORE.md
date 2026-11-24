# Integración con CORE

Actualizado con las respuestas de CORE del 10/11/2026 y del 18/11/2026 (fase 3: integración).
Separa lo confirmado, lo que falta implementar en este módulo y lo que todavía está pendiente.

## Confirmado por CORE

### Ruteo

| Tema | Acuerdo |
| --- | --- |
| Gateway | `https://core-api-production-e458.up.railway.app/api/v1/academic/*` |
| Destino | `ACADEMIC_SERVICE_URL` = `https://desarrolloaplicacionesiibackend-production.up.railway.app` (se la enviamos). |
| Reescritura | CORE **no** reescribe a `/api/`. Reenvía `/api/v1/academic/{resto}` a `/api/v1/{resto}`. Ejemplo: `/api/v1/academic/academica/carreras` llega como `/api/v1/academica/carreras`. |
| Endpoints | No se habilitan uno por uno: CORE reenvía todo lo que esté bajo el prefijo, con cualquier método, si trae un JWT válido. La autorización de cada operación es nuestra. |
| `/api/auth/*` | Para no exponerlo, basta con no moverlo a `/api/v1/`. |
| Health y OpenAPI | CORE no los consulta; no hace falta `/docs-json`. |
| Errores | El gateway devuelve nuestro status y body sin cambios. Timeout de 10 s. Tras 5 fallas de conexión seguidas corta 30 s con `503 GATEWAY_CIRCUIT_OPEN`. |
| Correlación | Hay que propagar el `X-Correlation-Id` recibido. |
| CORS | El navegador habla con CORE: nuestro backend no necesita CORS para el gateway. Si tenemos front propio, CORE agrega su origen a `CORS_ORIGINS`. |
| `X-Service-Name` | No se usa. El servicio que llama se identifica con el claim `service` del token de servicio. |

### JWT

| Campo | Valor |
| --- | --- |
| JWKS | `https://core-api-production-e458.up.railway.app/.well-known/jwks.json` |
| `iss` | `https://core-api-production-e458.up.railway.app` |
| `aud` | `uadenet` (el mismo para todos los módulos) |
| Algoritmo | RS256 |
| `kid` | Tomar la clave por el `kid` del token. Sin rotación automática; ante un `kid` desconocido, releer el JWKS. |
| Claims extra | `roles`, `careerIds`, `planIds`; en tokens de servicio, `service`. |

**Firma del gateway.** Como nuestro backend está expuesto públicamente en Railway, hay que
validar `X-Core-Signature`: HMAC-SHA256 en hexadecimal de estas líneas unidas por `\n`, sin
salto final:

```text
MÉTODO
ruta de destino (sin query)
X-User-Id
X-Correlation-Id
X-Core-Timestamp
```

El secreto va en `CORE_GATEWAY_SIGNATURE_SECRET`. `X-Core-Timestamp` viene en milisegundos
desde epoch; CORE no fija tolerancia y recomienda rechazar más de 5 minutos. La ruta es la que
recibimos nosotros, sin query (por ejemplo `/api/v1/academica/carreras`). Se compara en
tiempo constante.

Los claims: permisos en `permissions` (array), roles en `roles`; también llegan en
`X-User-Permissions` y `X-User-Roles`. El `sub` de personas es el `userId` de CORE (UUID), el
mismo que usan Backoffice, Alumnos y Docencia: es nuestro `alumnoId` y `docenteId`.

### Token de servicio

`POST https://core-api-production-e458.up.railway.app/internal/v1/service-auth/token` con JSON
`{ "clientId": "academic-service", "clientSecret": "..." }`. Responde
`{ "accessToken", "expiresIn": 900, "tokenType": "Bearer" }` (camelCase). Dura 15 minutos;
se renueva un minuto antes. El token trae `service: "academic"`, `sub: "service:academic"` y
los permisos `events:publish`, `webhooks:manage`, `backoffice:read`, `backoffice:people:read`.

### RabbitMQ

URL `amqp://academic:<contraseña>@<host>:<puerto>/uadenet` en `RABBITMQ_URL` (vhost `uadenet`,
usuario `academic`, sin TLS). El usuario sólo lee las colas `academic.*`: no puede publicar ni
declarar colas, exchanges o bindings (RabbitMQ cierra el canal). Un `nack` no activa reintentos
de CORE y con requeue el mensaje vuelve enseguida: los consumidores siempre hacen ack y guardan
el fallido en `eventos_recibidos` (columnas `payload` y `error`) para reprocesarlo.

### Envelope

Todos obligatorios salvo `causationId`: `specVersion` ("1.0"), `eventId` (UUID), `eventType`
(= routing key), `source`, `occurredAt` (ISO-8601), `subject`, `correlationId` (UUID),
`causationId` (UUID o null), `actor` (`{ type: user|service|system, id }`) y `data`.

### Publicación

Por HTTP, no al exchange: `POST /internal/v1/events` con el token de `academic-service` y el
envelope completo; `source` = `academic`. Responde 202. El mismo `eventId` con el mismo
contenido no se duplica; con contenido distinto responde 409. Obligatorios en `data`:

- `academic.exam-scheduled.v1`: `examId`, `subjectId`, `startAt`, `endAt`, `siteId`, `locationId`.
- `academic.term-defined.v1`: `termId`, `startDate`, `endDate`.

Se pueden sumar campos: CORE los distribuye sin cambios.

### Rutas internas para Backoffice

Backoffice llama directo a `/internal/v1/**`, sin gateway ni `X-Core-Signature`. Se valida el
JWT como siempre y además que el claim `service` sea exactamente `backoffice` (su token trae
`sub: "service:backoffice"` y `clientId: "backoffice-service"`). Los tokens de personas no
traen `service`.

### Permisos

- Escritura administrativa: rol `ACADEMIC_ADMIN` y permiso `academic:manage` (ya existen).
  No se crean `academic:read` ni `academic:write`.
- `STUDENT` y `TEACHER` no traen permisos académicos. Se autorizan por el claim `roles`
  (o `X-User-Roles`) y validando la propiedad del recurso: el alumno es el mismo `sub`, el
  docente dicta ese curso.

### Service account `academic-service`

Ya tiene `events:publish`, `webhooks:manage` y `backoffice:read`.

### Eventos

| Evento | Dirección | Cola o detalle |
| --- | --- | --- |
| `teacher.attendance.recorded.v1` | Consumimos | `academic.teacher-events.v1` (ya existe; usa `teacher.#`, filtrar por routing key). |
| `teacher.exam-result.published.v1` | Consumimos | `academic.teacher-events.v1`. |
| `backoffice.location.upserted.v1` | Consumimos | `academic.locations.v1` (creada por CORE). Implementado; ver `INTEGRACION_BACKOFFICE.md`. |
| `academic.exam-scheduled.v1` | Publicamos | En el catálogo; `source` = `academic`. |
| `academic.term-defined.v1` | Publicamos | En el catálogo; `source` = `academic`. |

Se consume con ack manual y deduplicando por `eventId`. No se declaran colas ni bindings
desde el código. No hace falta la cola `academic.regularity.v1` que habíamos propuesto.

## Estado de este módulo

| Área | Estado |
| --- | --- |
| Sincronización con Backoffice | **Implementada** detrás de `BACKOFFICE_ENABLED`. |
| Consumo de `backoffice.location.upserted.v1` | **Implementado** detrás de `BACKOFFICE_EVENTS_ENABLED`, con inbox `eventos_recibidos`. Usa `data.type`: un espacio nuevo que no es aula se ignora; un aula nueva dispara una sincronización completa. |
| Token de servicio | **Implementado** con el endpoint confirmado (JSON camelCase). `CORE_SERVICE_TOKEN` fijo sigue disponible para pruebas. |
| Prefijo `/api/v1/*` | **Implementado.** `/api/v1/academica`, `/api/v1/planificacion` y `/api/v1/regularidad`. `/api/auth/*` queda fuera. |
| Validación de JWT | **Implementada** para todo Bearer: RS256 por `kid` contra el JWKS, `iss` y `aud=uadenet`. Sin Bearer sigue valiendo la sesión local del frontend propio. |
| Firma del gateway | **Implementada** (`X-Core-Signature`, tolerancia `CORE_GATEWAY_SIGNATURE_TOLERANCE`, 5 min). Se activa al cargar `CORE_GATEWAY_SIGNATURE_SECRET`. Acepta timestamp en segundos, milisegundos o ISO-8601. |
| Autorización por rol y propiedad | **Implementada.** Ver la tabla de permisos más abajo. |
| Propagación de `X-Correlation-Id` | **Implementada:** se toma o genera, vuelve en toda respuesta, aparece en los logs y viaja en las llamadas a Backoffice. |
| Rutas `/internal/v1/**` | **Implementadas:** `carreras`, `carreras/con-ultimo-plan-activo`, `carreras/{id}/planes` y `planes/{id}/asignaturas`. Sólo `service=backoffice` (`CORE_INTERNAL_ALLOWED_SERVICES`). |
| Eventos `teacher.*` | **Consumo implementado** detrás de `TEACHER_EVENTS_ENABLED`: filtra `teacher.attendance.recorded.v1` y `teacher.exam-result.published.v1`, deduplica y guarda el mensaje completo en `eventos_recibidos.payload`. **Falta aplicarlos**: Docencia todavía no definió el contrato de `data`. |
| `academic.term-defined.v1` | **Implementado** detrás de `CORE_EVENTS_PUBLISH_ENABLED`: al crear un período se guarda en el outbox `eventos_publicados` y se envía a CORE cada 15 s, con reintentos. `termId` es el id del período. |
| `academic.exam-scheduled.v1` | **Implementado** detrás de `CORE_EVENTS_PUBLISH_ENABLED`: al crear una asignación de aula con `tipo: "EXAMEN"`. `examId` = id de la asignación, `subjectId` = id de la asignatura, `startAt`/`endAt` en ISO-8601 con hora de Argentina, `siteId`/`locationId` = UUID de Backoffice. Suma `subjectCode`, `subjectName`, `locationName`, `studentCount` y, si la fecha cae en un turno, `examTermId` y `examTermName`. Con la publicación activa, un examen en un aula sin ids de Backoffice se rechaza (400). |
| Analítica (legado) | El publicador Rabbit propio no cumple el contrato y no puede usarse con el RabbitMQ de CORE. |
| Frontend | Usa `/api/v1/**` y sigue llamando directo al backend con sesión local hasta tener el login de CORE. |

## Permisos por ruta

| Ruta | Quién |
| --- | --- |
| `GET /api/v1/academica/**`, `periodos`, `turnos-examen`, `cursos` (listado), `sedes/**`, agendas, `GET /api/v1/regularidad/cursos/{id}` | Administración, `STUDENT` y `TEACHER`. |
| `GET cursos/{id}`, `/docentes`, `/alumnos`, `/notas` | Administración o el docente asignado al curso. |
| `PUT cursos/{id}/notas/{alumnoId}`, `PUT cursos/{id}/inscripciones/{alumnoId}`, `PUT /api/v1/regularidad/cursos/{id}` | Administración o el docente asignado al curso. |
| `GET cursos/{id}/notas/{alumnoId}` | Administración, el docente del curso o el propio alumno. |
| `GET alumnos/{alumnoId}/inscripciones` | Administración o el propio alumno (`sub`). |
| `POST cursos/{id}/inscripciones` | Administración, o un `STUDENT` con `alumnoId` igual a su `sub`. |
| Todo lo demás bajo `/api/v1/**` | Administración: `ACADEMIC_ADMIN`, `academic:manage` o el `ADMINISTRATIVO` local. |
| `GET /internal/v1/**` | Sólo el token de servicio de Backoffice (claim `service` = `backoffice`). |

Un docente "dicta" un curso cuando su `sub` figura como `docenteId` en `curso_docentes`.

## Variables de entorno (Railway)

| Variable | Valor |
| --- | --- |
| `CORE_SERVICE_CLIENT_SECRET` | Secreto de `academic-service` (privado). |
| `CORE_GATEWAY_SIGNATURE_SECRET` | Secreto de `X-Core-Signature` (privado). |
| `RABBITMQ_URL` | `amqp://academic:<contraseña>@<host>:<puerto>/uadenet` (privado). |
| `BACKOFFICE_ENABLED` | `true` |
| `BACKOFFICE_EVENTS_ENABLED` | `true` |
| `TEACHER_EVENTS_ENABLED` | `true` |
| `CORE_EVENTS_PUBLISH_ENABLED` | `true` |
| `LOG_FORMAT` | `json` (logs filtrables en Railway; ver "Logs" en el README del backend) |

El resto (URLs de CORE, `CORE_SERVICE_CLIENT_ID`, `CORE_INTERNAL_ALLOWED_SERVICES`) ya tiene el
valor correcto por defecto. Antes del primer despliegue, correr
`docker/migrations/09-eventos-core.sql` y `10-asignacion-tipo.sql` si la base no usa
`DDL_AUTO=update`.

Los consumidores y la publicación se activan sólo en Railway: las colas son compartidas, así
que una instancia local consumiría mensajes destinados a producción.

## Siguiente paso

1. Acordar con Docencia el `data` de sus eventos y aplicarlos (asistencia y notas).
2. Migrar el frontend al login de CORE y al gateway (pasarle a CORE el origen real del front).

## Cómo pedir endpoints de otros módulos

Primero se pide el contrato al equipo dueño del dato y después se coordina con CORE el
acceso por gateway o por token de servicio. El pedido debe incluir: módulo y contacto,
caso de uso, método y ruta, request y response con ejemplos, errores, filtros y
paginación, identidad usada (usuario del portal o service account), permisos mínimos,
ambientes, timeout e idempotencia, y OpenAPI o JSON Schema.

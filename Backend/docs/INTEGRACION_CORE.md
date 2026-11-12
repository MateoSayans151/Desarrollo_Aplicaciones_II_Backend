# Integración con CORE

Actualizado con la respuesta de CORE del 10/11/2026 (fase 3: integración). Separa lo
confirmado, lo que falta implementar en este módulo y lo que todavía hay que pedirle a CORE.

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

El secreto lo pasa CORE por privado y va en una variable de entorno.

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
| Consumo de `backoffice.location.upserted.v1` | **Implementado** detrás de `BACKOFFICE_EVENTS_ENABLED`, con inbox `eventos_recibidos`. |
| Token de servicio | **Implementado**: token fijo (`CORE_SERVICE_TOKEN`) o client credentials (`CORE_SERVICE_TOKEN_URL`). Falta que CORE confirme el endpoint. |
| Prefijo `/api/v1/*` | **Implementado.** `/api/v1/academica`, `/api/v1/planificacion` y `/api/v1/regularidad`. `/api/auth/*` queda fuera. |
| Validación de JWT | **Implementada** para todo Bearer: RS256 por `kid` contra el JWKS, `iss` y `aud=uadenet`. Sin Bearer sigue valiendo la sesión local del frontend propio. |
| Firma del gateway | **Implementada** (`X-Core-Signature`, tolerancia `CORE_GATEWAY_SIGNATURE_TOLERANCE`, 5 min). Se activa al cargar `CORE_GATEWAY_SIGNATURE_SECRET`. Acepta timestamp en segundos, milisegundos o ISO-8601. |
| Autorización por rol y propiedad | **Implementada.** Ver la tabla de permisos más abajo. |
| Propagación de `X-Correlation-Id` | **Implementada:** se toma o genera, vuelve en toda respuesta, aparece en los logs y viaja en las llamadas a Backoffice. |
| Rutas `/internal/v1/**` | **Implementadas:** `carreras`, `carreras/con-ultimo-plan-activo`, `carreras/{id}/planes` y `planes/{id}/asignaturas`. Sólo tokens de servicio; se puede limitar con `CORE_INTERNAL_ALLOWED_SERVICES`. |
| Eventos `teacher.*` | Pendiente. |
| Publicación `academic.*` | Pendiente. El publicador Rabbit de Analítica es legado y no cumple el contrato. |
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
| `GET /internal/v1/**` | Sólo tokens de servicio (claim `service`). |

Un docente "dicta" un curso cuando su `sub` figura como `docenteId` en `curso_docentes`.

## Siguiente paso

1. Consumir `academic.teacher-events.v1` con el mismo esquema de inbox.
2. Publicar `academic.exam-scheduled.v1` y `academic.term-defined.v1`.
3. Migrar el frontend al login de CORE y al gateway.

Los tres dependen de datos que todavía tiene que pasar CORE.


## Pendiente de CORE

- Endpoint y formato para obtener el token de servicio de `academic-service` (client credentials).
- Datos de conexión a RabbitMQ (URL, vhost y credenciales) y formato del envelope de eventos.
- Mecanismo para publicar `academic.*` (endpoint HTTP o exchange) y campos obligatorios.
- Secreto de `X-Core-Signature` (por privado) y tolerancia de `X-Core-Timestamp`.
- Nombre exacto del claim de permisos y confirmación de que `alumnoId` y `docenteId` son el `sub` de CORE.

## Cómo pedir endpoints de otros módulos

Primero se pide el contrato al equipo dueño del dato y después se coordina con CORE el
acceso por gateway o por token de servicio. El pedido debe incluir: módulo y contacto,
caso de uso, método y ruta, request y response con ejemplos, errores, filtros y
paginación, identidad usada (usuario del portal o service account), permisos mínimos,
ambientes, timeout e idempotencia, y OpenAPI o JSON Schema.

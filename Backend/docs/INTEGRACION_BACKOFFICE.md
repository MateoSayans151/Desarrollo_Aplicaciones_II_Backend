# Integración con Backoffice (G8)

Backoffice es dueño del catálogo de sedes y espacios. Gestión Académica no lo administra:
lo lee por las rutas internas de Backoffice y guarda una copia local identificada por
`siteId` y `locationId`. El frontend ya no da de alta sedes ni aulas y sólo muestra las
vinculadas a Backoffice. Los endpoints locales de alta siguen en el backend para cargar
datos de prueba sin Backoffice y se bloquean al activar la integración.

## Contrato acordado (respuestas de G8, 10/11/2026 y 18/11/2026)

Base: `https://backend-integracion-41b2dbda.up.railway.app` (conectada al CORE real; aviso
de G8 del 18/11). Mismas rutas, mismo contrato (`openapi-internas.yaml`) y mismos ids de
sedes y espacios que el entorno anterior. La base anterior,
`https://backend-production-e464.up.railway.app`, sigue andando sólo con tokens del CORE
simulado hasta la entrega.

| Ruta interna | Equivale a | Filtros |
| --- | --- | --- |
| `GET /internal/v1/sites` | `/api/v1/backoffice/sites` | `status` (`ACTIVE`, `INACTIVE`) |
| `GET /internal/v1/locations` | `/api/v1/backoffice/locations` | `siteId`, `type`, `eventCapable`, `status` (`ACTIVE`, `INACTIVE`, `MAINTENANCE`) |

- **Paginación:** respuesta `{ items, nextCursor }`; parámetros `cursor` y `limit`
  (por defecto 20, máximo 100). `nextCursor: null` es la última página.
- **Sede:** `siteId`, `code`, `name`, `address`, `city`, `province` (nullable), `country`,
  `timezone`, `status`, `locationCount`, `updatedAt`.
- **Espacio:** `locationId`, `siteId`, `code`, `name`, `type`, `capacity`, `building` y
  `floor` (nullable), `accessible`, `eventCapable`, `equipment`, `status`.
- **Tipos de espacio:** `AULA`, `LABORATORIO`, `AUDITORIO`, `SALA_EVENTOS`, `COMEDOR`,
  `BIBLIOTECA`, `OFICINA`, `DEPOSITO`, `OTRO`.
- **`code`** lo genera Backoffice (por ejemplo `PIA1254`). Se guarda como texto; la relación
  es siempre por `locationId`.
- **Permiso:** token de servicio de `academic-service` con `backoffice:read` (card #78).
- **Cambios:** evento `backoffice.location.upserted.v1` con
  `{ locationId, siteId, name, capacity, status }`, en la cola `academic.locations.v1` que
  creó CORE (card #87).
- **Token:** la base nueva valida el token real de `academic-service`
  (`CORE_SERVICE_CLIENT_SECRET`). `CORE_SERVICE_TOKEN` sólo hace falta para apuntar a la base
  anterior con un token del CORE simulado.
- **Errores:** ante una falla, pasarle a G8 el `correlationId` de la respuesta.
- **Ficha de personas (`GET /internal/v1/users/{userId}`):** desde el 18/11 cada carrera
  suma `careerCode`, `careerName`, `planCode` y `planName` (pueden venir `null`); `careerId`
  y `planId` siguen siendo los nuestros. Hoy no consumimos la ficha, así que no nos afecta.
  Si la usáramos, el permiso es `backoffice:people:read`, que nuestro token ya trae.

## Cómo funciona

1. **Sincronización completa.** Con `BACKOFFICE_ENABLED=true`, el servicio lee todas las
   sedes y, por cada una, sus espacios `type=AULA`. Lo hace 20 segundos después de iniciar
   y luego cada `BACKOFFICE_SYNC_INTERVAL` (6 horas por defecto). También se puede forzar
   con `POST /api/v1/planificacion/sedes/sincronizar`, que devuelve un resumen de lo creado,
   actualizado, desactivado y descartado.
2. **Todo o nada.** Primero se leen todas las páginas por HTTP y después se aplica en una
   única transacción. Si Backoffice falla a mitad de camino no queda un catálogo parcial.
3. **Eventos.** Con `BACKOFFICE_EVENTS_ENABLED=true` se consume `academic.locations.v1`
   con ack manual. El `eventId` se registra en `eventos_recibidos` en la misma transacción
   que el cambio, así que un evento repetido no se aplica dos veces. Un mensaje que falla se
   guarda con su error en `eventos_recibidos` y se confirma igual (CORE no reintenta ante un
   nack). El código no declara colas ni bindings.
4. **Ubicaciones nuevas por evento.** Si llega un `locationId` desconocido con `type` `AULA`
   (o sin `type`), se relee el catálogo para traerla completa; otro tipo de espacio se ignora.

## Correspondencia con el modelo local

Se conservan las claves `Long` locales porque las usan cursos y reservas.

| Backoffice | Gestión Académica | Notas |
| --- | --- | --- |
| `siteId` | `Sede.backofficeSiteId` | Clave de la vinculación. |
| `name` | `Sede.nombre` | Si choca con otra sede local, se agrega `(code)`. |
| `address` | `Sede.direccion` | Si falta, se usa `city`. |
| `status` | `Sede.estado` | |
| `locationId` | `Aula.backofficeLocationId` | Clave de la vinculación. |
| `siteId` | `Aula.sede` | Se resuelve a la sede local vinculada. |
| `code` | `Aula.codigo` | Si choca con un aula local de la misma sede, se usa el `locationId`. |
| `name` | `Aula.nombre` | |
| `type` | `Aula.tipo` | Un tipo desconocido se guarda como `OTRO`. |
| `capacity` | `Aula.capacidadMaxima` | Un aula nueva sin capacidad positiva se descarta. |
| `status` | `Aula.estado` | `ACTIVE`, `INACTIVE` o `MAINTENANCE`. |

## Reglas de negocio

- Con la integración activa, `POST /api/v1/planificacion/sedes` y
  `POST /api/v1/planificacion/sedes/{id}/aulas` responden 400: el catálogo es de Backoffice.
- Sólo un aula `ACTIVE` de una sede `ACTIVE` admite cursos, cambios de horario o reservas
  nuevas. Lo ya planificado no se cancela automáticamente.
- Lo que Backoffice deja de informar no se borra: se marca `INACTIVE` para no perder la
  historia de cursos y reservas.
- Una sede mock con el mismo nombre que una sede de Backoffice se vincula a ella en lugar de
  duplicarse, así sus cursos quedan asociados a la sede real. Las aulas mock no se vinculan:
  quedan como estaban hasta que se recree la base.

## Configuración

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `BACKOFFICE_ENABLED` | `false` | Activa la sincronización y bloquea el alta local. |
| `BACKOFFICE_BASE_URL` | `https://backend-integracion-41b2dbda.up.railway.app` | Base de Backoffice (CORE real). |
| `BACKOFFICE_SYNC_INTERVAL` | `PT6H` | Intervalo entre sincronizaciones (ISO-8601). |
| `BACKOFFICE_EVENTS_ENABLED` | `false` | Consume `backoffice.location.upserted.v1`. |
| `BACKOFFICE_EVENTS_QUEUE` | `academic.locations.v1` | Cola creada por CORE. |
| `CORE_SERVICE_TOKEN` | vacío | Token fijo; tiene prioridad. Sólo para la base anterior con el CORE simulado. |
| `CORE_SERVICE_TOKEN_URL` | `https://core-api-production-e458.up.railway.app/internal/v1/service-auth/token` | Endpoint de CORE (JSON `clientId`/`clientSecret`). |
| `CORE_SERVICE_CLIENT_ID` | `academic-service` | Service account. |
| `CORE_SERVICE_CLIENT_SECRET` | vacío | Sólo en Railway y en el `.env` local, nunca en Git. |
| `RABBITMQ_URL` | vacío | `amqp://academic:<contraseña>@<host>:<puerto>/uadenet`, entregada por CORE. |

## Base de datos

La tabla `eventos_recibidos` está en `docker/init/01-schema-and-data.sql` para volúmenes
nuevos. En una base existente con el perfil `local` (que valida el esquema), aplicar
`docker/migrations/08-integracion-backoffice.sql`. Con `DDL_AUTO=update` Hibernate la
crea solo.

## Rutas que Backoffice nos consume

Base: `https://desarrolloaplicacionesiibackend-production.up.railway.app`. Se llaman directo
(sin gateway) con el token de servicio de CORE; basta con que el token traiga el claim
`service`. CORE indicó que no hace falta un permiso `academic:read`.

| Ruta | Respuesta |
| --- | --- |
| `GET /internal/v1/carreras` | `Carrera[]` |
| `GET /internal/v1/carreras/con-ultimo-plan-activo` | `Carrera[]` con `ultimoPlanActivo` |
| `GET /internal/v1/carreras/{carreraId}/planes` | `Plan[]` |
| `GET /internal/v1/planes/{planId}/asignaturas` | `Asignatura[]`: `{ id, codigo, nombre, anio, creditos, cargaHoraria, estado, planId }` |

No publicamos un evento al activar un plan: el catálogo de CORE sólo nos asigna
`academic.exam-scheduled.v1` y `academic.term-defined.v1`. Backoffice puede consultar
`/internal/v1/carreras/con-ultimo-plan-activo` cuando lo necesite.

## Pendiente con Backoffice

- Nada bloqueante. Ya validan el token del CORE real (18/11) y el claim `service` de su
  token es `backoffice` (confirmado por CORE), que es el valor por defecto de
  `CORE_INTERNAL_ALLOWED_SERVICES`.
- En Railway, sacar `BACKOFFICE_BASE_URL` si estaba fijada a la base anterior (o
  apuntarla a la nueva) y `CORE_SERVICE_TOKEN` si quedó el token simulado.

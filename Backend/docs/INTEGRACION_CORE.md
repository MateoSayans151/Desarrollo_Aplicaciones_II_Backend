# Integración con CORE: estado y pedidos pendientes

Revisión realizada el 23 de octubre de 2026 tomando como referencia el contrato CORE
v1 del 5 de agosto de 2026. Este documento separa lo que funciona localmente de lo que
requiere coordinación con CORE u otro módulo.

## Estado comprobado

| Área | Estado actual |
| --- | --- |
| API local | Implementada bajo `/api/**`, con PostgreSQL y sesión `JSESSIONID`. |
| Frontend | Todas las pantallas activas usan la API local; no queda modo mock ni datos en `localStorage`. |
| Health | Implementados `GET /health/live` y `GET /health/ready`; readiness devuelve `503 DOWN` si PostgreSQL no responde. |
| OpenAPI | Disponible en `/v3/api-docs` y Swagger UI en `/swagger-ui.html`. |
| Gateway CORE | Pendiente. El contrato espera exponer `/api/v1/academic/*` y reescribir a `/api/v1/*`, pero el backend recibe `/api/**`. |
| Autenticación CORE | Pendiente. La aplicación usa login local; todavía no valida JWT RS256 de CORE. |
| Errores CORE | Los errores de aplicación usan `application/problem+json`; falta unificar los `401`/`403` de Spring Security. |
| Correlación | Los errores conservan o generan `correlationId`; falta propagarlo sistemáticamente en logs, llamadas y eventos. |
| Eventos publicados | Existe un publicador Rabbit opcional legado para resultados; no cumple el envelope ni la topología de CORE. |
| Eventos consumidos | Pendientes `teacher.attendance.recorded.v1` y `teacher.exam-result.published.v1`. |
| Backoffice | El modelo guarda UUID externos, pero falta sincronizar sites y locations. |

## Qué pedirle a CORE para habilitar nuestros endpoints

Enviar un pedido con estos datos:

1. **Alta del servicio académico** con el valor definitivo de `ACADEMIC_SERVICE_URL`
   para cada ambiente.
2. **Decisión de reescritura de rutas**. Nuestro backend recibe `/api/**`; el contrato
   del gateway reescribe `/api/v1/academic/*` a `/api/v1/*`. CORE debe confirmar una de
   estas opciones:
   - configurar temporalmente la reescritura hacia `/api/*`; o
   - acordar el cambio del backend a `/api/v1/*` y la ruta pública exacta de cada recurso.
3. **URL base del gateway** para desarrollo, integración y entrega.
4. **Datos de validación JWT**: JWKS URL, `issuer`, `audience`, algoritmo, nombres de
   claims y rotación de claves.
5. **Roles y permisos académicos** que enviará CORE y su correspondencia con lectura y
   escritura. Hoy se aceptan `ADMINISTRATIVO`, `ADMINISTRADOR` y `ACADEMIC_ADMIN`.
6. **Credenciales de servicio**: `clientId`, `clientSecret`, `X-Service-Name` esperado y
   scopes autorizados. Deben entregarse fuera de Git.
7. **CORS y orígenes permitidos** del portal que llamará al gateway.
8. **Registro del OpenAPI** y confirmación de las URLs públicas de documentación. El
   contrato CORE menciona `/docs` y `/docs-json`; hoy Spring expone otras rutas.
9. **Health checks del gateway** contra `/health/live` y `/health/ready`, incluidos
   intervalos y timeout.
10. **Formato de errores de seguridad** para que los `401` y `403` también incluyan
    `code` y `correlationId`.

### Texto listo para enviar a CORE

> Necesitamos dar de alta Gestión Académica en el gateway. Por favor confirmen
> `ACADEMIC_SERVICE_URL` y la URL del ambiente, y definan cómo se mapeará
> `/api/v1/academic/*` porque hoy nuestro servicio recibe `/api/*` y el contrato v1
> indica reescritura a `/api/v1/*`. También necesitamos JWKS URL, issuer, audience,
> claims, roles/permisos académicos, credenciales y scopes del service account,
> orígenes CORS y configuración de health checks. Adjuntamos el OpenAPI generado en
> `/v3/api-docs` y el inventario de endpoints. Las credenciales deben enviarse por un
> canal seguro y no en el repositorio.

## Eventos que debe coordinar Gestión Académica

Según el catálogo CORE, este módulo debe:

- consumir `teacher.attendance.recorded.v1`;
- consumir `teacher.exam-result.published.v1`;
- publicar `academic.exam-scheduled.v1`;
- publicar `academic.term-defined.v1`.

El publicador local `resultado.publicado` no reemplaza esos eventos. Antes de activarlo
en un ambiente compartido hay que retirarlo o adaptarlo.

Para consumir eventos se debe abrir un PR en el repositorio de contratos de CORE:

```yaml
queues:
  - name: academic.regularity.v1
    durable: true
    owner: academic
    bindings:
      - exchange: uadenet.events.v1
        routingKeys:
          - teacher.attendance.recorded.v1
          - teacher.exam-result.published.v1
    deadLetter: uadenet.dlx.v1
    maxRetries: 3
```

El nombre final de la cola debe aprobarlo CORE. La implementación consumidora tendrá
que usar ack manual, una inbox con índice único por `eventId` y persistir el efecto y la
deduplicación en la misma transacción.

Para publicar los dos eventos académicos hay que solicitar o confirmar:

- los JSON Schema en `contracts/events/`;
- las entradas de AsyncAPI;
- el scope de publicación del service account académico;
- el endpoint `POST /internal/v1/events` y su URL de ambiente;
- las reglas para `subject`, `actor`, `correlationId` e `Idempotency-Key`.

## Cómo solicitar endpoints de otros módulos

Primero se pide el contrato al equipo propietario del dato. Después se solicita a CORE
el acceso correspondiente por gateway o por credenciales de servicio. El pedido debe
incluir:

- módulo propietario y contacto;
- caso de uso concreto;
- método y ruta solicitados;
- request, response y códigos de error con ejemplos;
- filtros, paginación y límites;
- identidad usada: usuario del portal o service account;
- permisos o scopes mínimos;
- ambientes y URL base;
- timeout e idempotencia, si corresponde;
- OpenAPI o JSON Schema versionado.

### Solicitud para Backoffice

Gestión Académica necesita leer sedes y aulas. Pedir a Backoffice y CORE:

- endpoint interno paginado para sites;
- endpoint interno paginado para locations filtrable por `siteId` y `type=AULA`;
- schemas de `siteId`, `locationId`, nombre, dirección, capacidad, tipo y estado;
- scope de lectura para `academic-service`;
- URL interna a través de CORE y política de caché/sincronización;
- comportamiento ante bajas y cambios de capacidad.

La ruta pública `/api/v1/backoffice/locations` no alcanza para una llamada
backend a backend. Debe confirmarse la equivalente `/internal/v1/**` o el mecanismo
oficial que CORE autorice.

### Texto listo para pedir un endpoint a otro equipo

> Desde Gestión Académica necesitamos consultar **[recurso]** para **[caso de uso]**.
> ¿Pueden compartir el método y la ruta oficial, filtros, paginación, request/response,
> códigos de error, OpenAPI y ambiente? La llamada será **[desde el portal con token de
> usuario / desde academic-service con token de servicio]**. También necesitamos que
> CORE confirme el prefijo del gateway y el scope mínimo para habilitarla.

## Condiciones para declarar la integración terminada

- El gateway alcanza todos los endpoints acordados y no expone el backend directamente.
- El backend valida JWT de CORE y mapea permisos sin depender de la sesión local.
- El frontend usa login, refresh y logout de CORE con el access token solo en memoria.
- Se propaga `X-Correlation-Id` y los errores usan `application/problem+json`.
- Readiness verifica sus dependencias obligatorias.
- OpenAPI refleja las rutas efectivamente publicadas.
- Los eventos cumplen los schemas y la topología de CORE.
- La sincronización con Backoffice usa contratos y credenciales aprobados.

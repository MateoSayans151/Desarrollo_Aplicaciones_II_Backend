# Gestión Académica y Planificación

Backend MVC/REST construido con Spring Boot 3, Java 17, Spring Data JPA, Spring Security y PostgreSQL (Supabase).

## Integración con CORE

Las respuestas de cursos incluyen `sedeNombre` y `aulaNombre` además de `sedeId`
y `aulaId`. Los nombres se leen de las tablas `sedes` y `aulas`; no se agregan
columnas a `cursos`.

Conviven dos formas de autenticarse: el JWT de CORE (Bearer), que llega por el gateway a
`/api/v1/**` o directo a `/internal/v1/**`, y la sesión local que usa el frontend propio
hasta migrar al login de CORE. Permisos y firma del gateway: [`docs/INTEGRACION_CORE.md`](docs/INTEGRACION_CORE.md).
La API de notas usa el nombre de campo compartido `alumnoId` para identificar al alumno en las inscripciones.

El inventario exhaustivo, las reglas de negocio y las diferencias entre el estado
actual y el contrato objetivo de CORE están en
[`docs/CONTRATO_PORTALES_ALUMNOS_Y_DOCENCIA.md`](docs/CONTRATO_PORTALES_ALUMNOS_Y_DOCENCIA.md).
Los datos que se deben pedir a CORE, el texto de solicitud y el procedimiento para
consumir APIs de otros módulos están en
[`docs/INTEGRACION_CORE.md`](docs/INTEGRACION_CORE.md).
El recorrido visual está en el
[`mapa interactivo de arquitectura`](docs/arquitectura-interactiva/index.html).

Valores de JWT confirmados por CORE (ya son los valores por defecto). La firma del gateway
se activa con `CORE_GATEWAY_SIGNATURE_SECRET`:

```text
CORE_JWT_JWK_SET_URI=https://core-api-production-e458.up.railway.app/.well-known/jwks.json
CORE_JWT_ISSUER=https://core-api-production-e458.up.railway.app
CORE_JWT_AUDIENCE=uadenet
```

## Integración con Backoffice

El catálogo de sedes y aulas es de Backoffice. Con `BACKOFFICE_ENABLED=true` el servicio lo
sincroniza desde `/internal/v1/sites` y `/internal/v1/locations?type=AULA`, bloquea el alta
local de sedes y aulas, y sólo admite cursos y reservas en aulas `ACTIVE`. La sincronización
se puede forzar con `POST /api/v1/planificacion/sedes/sincronizar`. Necesita el token de
servicio de CORE: alcanza con `CORE_SERVICE_CLIENT_SECRET` (o un token fijo en `CORE_SERVICE_TOKEN`).
Detalle completo en [`docs/INTEGRACION_BACKOFFICE.md`](docs/INTEGRACION_BACKOFFICE.md); eventos,
RabbitMQ y variables de Railway en [`docs/INTEGRACION_CORE.md`](docs/INTEGRACION_CORE.md).

Los endpoints de este servicio se implementan bajo `/api/v1/planificacion`,
`/api/v1/academica` y otros controladores del módulo. La ruta externa del gateway
se configura por separado. Las comprobaciones de vida están en `GET /health/live`
y `GET /health/ready`; readiness comprueba la conexión con PostgreSQL.

Todos los errores, incluidos los `401` y `403` de Spring Security, usan
`application/problem+json` e incluyen `code` y `correlationId`. Sin sesión la API
responde `401`; con sesión pero sin permiso, `403`.

Los códigos nuevos de carreras, planes, asignaturas y cursos usan el formato
`ARQ-001` o `ARQ1-001` (`^[A-Z]{3}[0-9]?-[0-9]{3}$`). La API rechaza con HTTP 400
los códigos que no respetan este contrato.

Una materia sólo puede configurarse una vez como correlativa de otra. Los intentos de
repetirla con el mismo tipo o con un tipo diferente responden HTTP 400 como regla de
negocio, en lugar de propagar un error de persistencia. Tampoco se aceptan ciclos ni
correlativas de un año posterior al de la asignatura.

Los planes nacen en `PLANIFICADO` (borrador) y sólo pueden pasar a `ACTIVO` si tienen
asignaturas y su vigencia no pertenece a un año futuro: pueden convivir planes activos
del año actual y de años anteriores junto con el borrador de un plan futuro. Un plan
activo no vuelve a planificado ni se elimina. Sólo las asignaturas de planes activos
de carreras no inactivas admiten cursos y reservas de aula.

Al crear un aula con `POST /api/v1/planificacion/sedes/{id}/aulas`, se puede enviar
`nombre` además de `codigo` y `capacidadMaxima` (entre 1 y 100). Si se omite, se utiliza su código.
Las sedes ya tienen nombre. La base existente no necesita migración para exponerlos
en las respuestas de cursos.

## Estructura

- `Entidades`: modelo persistente del dominio.
- `Repositorios`: acceso a datos con Spring Data JPA.
- `BusinessLogic`: reglas y casos de uso.
- `Controllers`: endpoints HTTP y manejo uniforme de errores.
- `Modelos`: DTO de entrada y salida expuestos a los clientes, sin información sensible.
- `Configuracion`: seguridad y documentación OpenAPI.
- `Excepciones`: errores propios de las reglas de negocio.

## Funcionalidades

- Carreras con duración calculada según sus materias, planes de estudio con estado, asignaturas y correlatividades.
- Sedes, aulas, capacidad, agenda y detección de superposición horaria.
- Cuatrimestres y turnos de examen final.
- Cursos/comisiones por cuatrimestre, con docentes, inscripciones y horarios de aula.
- Condiciones de regularidad persistidas por curso, consultables sin identificar alumnos.
- Usuarios persistidos, login por sesión y autorización por permiso administrativo.

## Ejecución

Requiere JDK 17 y Maven 3.9 o superior.

### Conectar a Supabase

Configurá `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en `.env` con los datos
de **Supabase > Connect**. La plantilla `.env.supabase.example` muestra el formato
esperado para JDBC. Usá el host, puerto y usuario que te da el panel; no copies
la URI `postgresql://...` sin convertirla a `jdbc:postgresql://...`.

Para Supabase, no actives el perfil `local`; dejá comentada
`SPRING_PROFILES_ACTIVE=local`. Iniciá desde esta carpeta:

```powershell
.\mvnw.cmd spring-boot:run
```

La base debe tener el esquema vigente. En particular, `periodos_academicos`
incluye `inscripcion_desde` e `inscripcion_hasta`, ambas fechas obligatorias.
Al recuperar una base creada con una versión anterior, aplicá una migración de
datos antes de usar `DDL_AUTO=update`: PostgreSQL no puede agregar una columna
obligatoria sobre períodos existentes sin asignarles un valor.

`DDL_AUTO=update` no modifica restricciones existentes. Una base creada antes de los
estados `PLANIFICADO`/`ACTIVO` necesita `docker/migrations/07-plan-estados-y-correlatividad-unica.sql`;
sin ella, crear un plan falla por el check de `planes_estudio`. Con las tablas
académicas vacías, `docker/init/02-datos-prueba.sql` carga datos de
demostración coherentes con las reglas de negocio, incluidos planes 2025 y 2026 activos
y un plan 2027 planificado. Sus fechas fijas fueron pensadas para fines de 2026.

### Conectar al PostgreSQL de Docker

Desde esta carpeta, iniciá Docker y el perfil local:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Este perfil usa las variables `LOCAL_DB_*` de `.env`. El contenedor crea las tablas y carga
los datos de prueba automáticamente la primera vez que se levanta (ver `docker/README.md`).

La API queda disponible en `http://localhost:8080`. Por defecto utiliza:

- Usuario: `admin@universidad.edu.ar`
- Clave: `CambiarEstaClave123!`

### Swagger UI

Con la aplicación en ejecución, se puede abrir:

`http://localhost:8080/swagger-ui.html`

Se puede iniciar sesión mediante `POST /api/auth/login`. La sesión queda asociada a la
cookie `JSESSIONID`. También se puede usar **Authorize** con un JWT de CORE; HTTP Basic no
está activo.
La especificación OpenAPI en formato JSON está disponible en `http://localhost:8080/v3/api-docs`.

Estas credenciales son únicamente para desarrollo. En un entorno real se deben definir
`UNIVERSIDAD_ADMIN_EMAIL`, `UNIVERSIDAD_ADMIN_PASSWORD` y `UNIVERSIDAD_DOMINIO_EMAIL`, o
reemplazar la autenticación local por OAuth2/SSO de la universidad.

## Endpoints principales

| Método | Ruta | Operación |
|---|---|---|
| POST / GET | `/api/v1/academica/carreras` | Crear/listar carreras |
| GET | `/api/v1/academica/carreras/con-ultimo-plan-activo` | Listar carreras con su plan activo de vigencia más reciente |
| PUT / DELETE | `/api/v1/academica/carreras/{id}` | Editar/desactivar carrera |
| POST / GET | `/api/v1/academica/carreras/{id}/planes` | Crear/listar planes |
| PUT / DELETE | `/api/v1/academica/planes/{id}` | Editar o activar un plan/eliminar un plan planificado sin cursos ni reservas |
| POST / GET | `/api/v1/academica/planes/{id}/asignaturas` | Crear/listar asignaturas |
| POST / GET | `/api/v1/academica/asignaturas/{id}/correlatividades` | Crear correlativas y consultar condiciones agrupadas por tipo; cada materia incluye ID, código y nombre |
| POST / GET | `/api/v1/planificacion/sedes` | Crear/listar sedes |
| POST / GET | `/api/v1/planificacion/sedes/{id}/aulas` | Crear/listar aulas; nombre opcional al crear |
| POST | `/api/v1/planificacion/sedes/sincronizar` | Leer el catálogo de Backoffice (requiere `BACKOFFICE_ENABLED=true`) |
| POST / GET | `/api/v1/planificacion/asignaciones` | Crear/listar asignaciones de aulas |
| GET | `/api/v1/planificacion/aulas/{id}/agenda?fecha=AAAA-MM-DD` | Consultar agenda diaria unificada de un aula (cursos y asignaciones puntuales) |
| GET | `/api/v1/planificacion/aulas/{id}/agenda?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Consultar cursos y asignaciones puntuales de un aula dentro de un rango inclusivo |
| GET | `/api/v1/planificacion/asignaturas/{id}/agenda?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Consultar horarios ocupados por una asignatura en el rango indicado |
| POST / GET | `/api/v1/planificacion/periodos` | Crear/listar cuatrimestres |
| POST / GET | `/api/v1/planificacion/cursos` | Crear/listar cursos; admite `?periodoId=`. Al crear requiere horario, aula, `asistenciaMinima` y `promedioMinimo`. |
| GET | `/api/v1/planificacion/cursos/{id}` | Consultar curso, docentes e inscripciones |
| GET | `/api/v1/planificacion/cursos/{id}/docentes` | Listar docentes asignados al curso |
| GET | `/api/v1/planificacion/cursos/{id}/alumnos` | Listar alumnos inscriptos y sus resultados |
| PUT | `/api/v1/planificacion/cursos/{id}/estado` | Actualizar estado del curso |
| POST | `/api/v1/planificacion/cursos/{id}/docentes` | Asignar docente externo al curso |
| POST | `/api/v1/planificacion/cursos/{id}/inscripciones` | Inscribir alumno |
| PUT | `/api/v1/planificacion/cursos/{id}/inscripciones/{alumnoId}` | Actualizar estado o resultado de la inscripción |
| GET | `/api/v1/planificacion/alumnos/{alumnoId}/inscripciones?periodoId={id}` | Listar inscripciones de un alumno; el período es opcional |
| GET | `/api/v1/planificacion/cursos/{id}/notas` | Listar notas finales de alumnos inscriptos |
| GET / PUT | `/api/v1/planificacion/cursos/{id}/notas/{alumnoId}` | Consultar o cargar la nota final de un alumno |
| PUT | `/api/v1/planificacion/cursos/{id}/horario` | Actualizar día, aula y horas del curso |
| POST / GET | `/api/v1/planificacion/turnos-examen` | Crear/listar turnos |
| GET | `/api/v1/regularidad/cursos/{cursoId}` | Consultar los mínimos de regularidad del curso, sin datos de alumno |
| PUT | `/api/v1/regularidad/cursos/{cursoId}` | Crear o actualizar los mínimos de regularidad del curso |
| POST | `/api/auth/login` | Iniciar sesión local |
| POST | `/api/auth/logout` | Cerrar sesión local |
| GET | `/api/auth/me` | Consultar usuario autenticado |

Salvo health, Swagger y login, todas las rutas requieren una sesión local o un JWT de CORE.
La administración (`ADMINISTRATIVO` local, `ACADEMIC_ADMIN` o `academic:manage`) accede a
todo `/api/v1/**`. Con JWT de CORE, `STUDENT` y `TEACHER` leen los catálogos y sólo acceden
a sus propios datos: el alumno a sus inscripciones y notas, el docente a los cursos que dicta.
`/internal/v1/**` sólo admite tokens de servicio. Un usuario local con permiso `USUARIO`
recibe HTTP 403, aunque puede consultar `/api/auth/me` y cerrar su sesión. La tabla completa
está en [`docs/INTEGRACION_CORE.md`](docs/INTEGRACION_CORE.md).

### Ejemplo de login

```json
{
  "email": "admin@universidad.edu.ar",
  "password": "CambiarEstaClave123!"
}
```

### Ejemplo de condiciones de regularidad

`GET /api/v1/regularidad/cursos/42` devuelve:

```json
{
  "id": 7,
  "cursoId": 42,
  "codigo": "DDA2-001",
  "asistenciaMinima": 75,
  "promedioMinimo": 6
}
```

La consulta no recibe `alumnoId`, asistencia obtenida ni promedio obtenido. Las
correlativas se consultan con
`GET /api/v1/academica/asignaturas/{asignaturaId}/correlatividades`. La migración
`docker/migrations/06-regularidad-por-curso.sql` crea la tabla y completa con 75% y
6.0 los cursos que todavía no tienen configuración.

## Pruebas

```bash
mvn test
```

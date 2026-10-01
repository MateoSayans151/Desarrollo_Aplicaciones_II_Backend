# Gestión Académica y Planificación

Backend MVC/REST construido con Spring Boot 3, Java 17, Spring Data JPA, Spring Security y PostgreSQL (Supabase).

## Integración con CORE

Este servicio no expone login propio: recibe JWT de usuario validados con el JWKS de CORE.
Definí estas variables fuera de Git antes de desplegar:

```text
CORE_JWT_JWK_SET_URI=https://core/.well-known/jwks.json
CORE_JWT_ISSUER=https://core
CORE_JWT_AUDIENCE=academic-service
```

Los endpoints de este servicio se implementan bajo `/api/planificacion`,
`/api/academica` y otros controladores del módulo. La ruta externa del gateway
se configura por separado. Las comprobaciones de vida están en `GET /health/live`
y `GET /health/ready`.

## Estructura

- `Entidades`: modelo persistente del dominio.
- `Repositorios`: acceso a datos con Spring Data JPA.
- `BusinessLogic`: reglas y casos de uso.
- `Controllers`: endpoints HTTP y manejo uniforme de errores.
- `Modelos`: DTO de entrada y salida expuestos a los clientes, sin información sensible.
- `Configuracion`: seguridad y documentación OpenAPI.
- `Excepciones`: errores propios de las reglas de negocio.

## Funcionalidades

- Carreras, planes de estudio, asignaturas y correlatividades.
- Sedes, aulas, capacidad, agenda y detección de superposición horaria.
- Cuatrimestres y turnos de examen final.
- Cursos/comisiones por cuatrimestre, con docentes, inscripciones y horarios de aula.
- Validación de regularidad por asistencia y promedio.
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

### Conectar al PostgreSQL de Docker

Desde esta carpeta, iniciá Docker y el perfil local:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Este perfil usa las variables `LOCAL_DB_*` de `.env`.

La API queda disponible en `http://localhost:8080`. Por defecto utiliza:

- Usuario: `admin@universidad.edu.ar`
- Clave: `CambiarEstaClave123!`

### Swagger UI

Con la aplicación en ejecución, se puede abrir:

`http://localhost:8080/swagger-ui.html`

Se puede iniciar sesión mediante `POST /api/usuarios/login`. También es posible presionar
**Authorize** e ingresar el usuario y la clave indicados arriba para utilizar HTTP Basic.
La especificación OpenAPI en formato JSON está disponible en `http://localhost:8080/v3/api-docs`.

Estas credenciales son únicamente para desarrollo. En un entorno real se deben definir
`UNIVERSIDAD_ADMIN_EMAIL`, `UNIVERSIDAD_ADMIN_PASSWORD` y `UNIVERSIDAD_DOMINIO_EMAIL`, o
reemplazar la autenticación en memoria por OAuth2/SSO de la universidad.

### Usuario admin de prueba

Además del admin sembrado por defecto, existe un usuario admin de prueba creado a mano
para testear la API:

- Usuario: `admin.prueba@universidad.edu.ar`
- Clave: `AdminPrueba123!`

## Endpoints principales

| Método | Ruta | Operación |
|---|---|---|
| POST / GET | `/api/academica/carreras` | Crear/listar carreras |
| PUT / DELETE | `/api/academica/carreras/{id}` | Editar/desactivar carrera |
| POST / GET | `/api/academica/carreras/{id}/planes` | Crear/listar planes |
| GET | `/api/academica/planes/{id}/pdf` | Descargar el plan de estudios (con asignaturas y correlatividades) en PDF |
| POST / GET | `/api/academica/planes/{id}/asignaturas` | Crear/listar asignaturas |
| POST / GET | `/api/academica/asignaturas/{id}/correlatividades` | Gestionar correlativas |
| POST / GET | `/api/planificacion/sedes` | Crear/listar sedes |
| POST / GET | `/api/planificacion/sedes/{id}/aulas` | Crear/listar aulas |
| POST | `/api/planificacion/asignaciones` | Asignar un aula |
| GET | `/api/planificacion/aulas/{id}/agenda?fecha=AAAA-MM-DD` | Consultar agenda |
| POST / GET | `/api/planificacion/periodos` | Crear/listar cuatrimestres |
| POST / GET | `/api/planificacion/cursos` | Crear/listar cursos; admite `?periodoId=` |
| GET | `/api/planificacion/cursos/{id}` | Consultar curso, docentes, inscripciones y horarios |
| PUT | `/api/planificacion/cursos/{id}/estado` | Actualizar estado del curso |
| POST | `/api/planificacion/cursos/{id}/docentes` | Asignar docente externo al curso |
| POST / PUT | `/api/planificacion/cursos/{id}/inscripciones` | Inscribir alumno o actualizar su resultado |
| POST | `/api/planificacion/cursos/{id}/horarios` | Agregar aula y horario semanal |
| POST / GET | `/api/planificacion/turnos-examen` | Crear/listar turnos |
| POST | `/api/regularidad/validar` | Validar habilitación a final |
| POST | `/api/usuarios/login` | Iniciar sesión |
| POST | `/api/usuarios/logout` | Cerrar sesión |
| GET | `/api/usuarios/actual` | Consultar usuario autenticado |
| POST / GET | `/api/usuarios` | Crear/listar usuarios administrativos |

Salvo el login, todas las rutas requieren una sesión válida o autenticación HTTP Basic.
Las operaciones de lectura (`GET`) bajo `/api/academica/**` y `/api/planificacion/**`
(carreras, planes, asignaturas, correlatividades, sedes, aulas, agenda, períodos y turnos,
incluyendo la descarga del PDF del plan) están disponibles tanto para `ADMINISTRATIVO` como
para `USUARIO`. Cualquier escritura (`POST`/`PUT`/`DELETE`) en esas rutas, y el resto de la
API (`/api/usuarios/**`, `/api/regularidad/**`), exige el permiso `ADMINISTRATIVO`; un usuario
con permiso `USUARIO` recibirá HTTP 403 al intentarlo.

### Ejemplo de login

```json
{
  "email": "admin@universidad.edu.ar",
  "password": "CambiarEstaClave123!"
}
```

### Ejemplo de validación de regularidad

```json
{
  "asistencia": 80,
  "promedio": 7,
  "asistenciaMinima": 75,
  "promedioMinimo": 6
}
```

## Pruebas

```bash
mvn test
```

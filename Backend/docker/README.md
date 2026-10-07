# Base local de desarrollo

Desde `Backend/Backend`, crear e iniciar PostgreSQL:

```powershell
docker compose up -d
```

El contenedor queda disponible en `localhost:5432`, con base `gestion_academica`,
usuario `gestion_academica` y clave `gestion_academica_dev`.

Supabase sigue siendo el comportamiento predeterminado: no actives el perfil `local`
y conservá las variables `DB_*` de Supabase en `.env`. Para ejecutar contra Docker,
configurá las variables `LOCAL_DB_*` de `.env.docker.example` (sin reemplazar las de
Supabase) y arrancá explícitamente con el perfil `local`:

```powershell
./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

El SQL de `init/01-schema-and-data.sql` se ejecuta al crear el volumen por primera vez.
Para reinicializar los datos de desarrollo se debe bajar el compose con su volumen y
volver a levantarlo:

```powershell
docker compose down -v
docker compose up -d
```

Usuario de prueba: `admin@universidad.edu.ar` / `CambiarEstaClave123!`.

Las columnas para vincular sedes y aulas con Backoffice ya están en el esquema
de volúmenes nuevos. Para una base existente, aplicar manualmente
`docker/migrations/02-backoffice-sites-and-locations.sql`.

El campo `asignaturas.anio` ahora es numerico, en concordancia con la entidad
`Asignatura`. Para una base existente, aplicar manualmente
`docker/migrations/03-asignatura-anio-integer.sql`. La migracion requiere que
los valores almacenados sean niveles numericos.

Para una base existente, aplicar `docker/migrations/04-curso-turno-y-horas.sql`
antes de iniciar el backend actualizado. Agrega `turno`, `hora_inicio` y
`hora_fin` a `cursos`. Los cursos existentes quedan con valores nulos hasta
que se conozcan sus horarios; la API exige los tres campos al crear cursos nuevos.

### Cambios de esquema

El período académico ahora incluye `inscripcionDesde` e `inscripcionHasta`.
Si el volumen `postgres_data` ya existía antes de este cambio y es sólo de
desarrollo, recrealo con los comandos anteriores. Esto borra los datos locales;
no lo uses para una base con información que debas conservar.

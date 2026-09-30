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

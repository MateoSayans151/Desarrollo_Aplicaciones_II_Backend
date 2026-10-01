# Integración futura con Backoffice

La conexión todavía no está activa. application.properties contiene las propiedades comentadas para configurar la URL base y las rutas cuando Backoffice publique un entorno accesible.

## Formato de Backoffice

Sedes:

~~~json
{
  "items": [
    {
      "siteId": "3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90",
      "name": "string",
      "address": "string",
      "status": "ACTIVE"
    }
  ],
  "nextCursor": null
}
~~~

Ubicaciones:

~~~json
{
  "items": [
    {
      "locationId": "c41a77b2-0000-0000-0000-000000000000",
      "siteId": "3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90",
      "name": "Aula 305",
      "type": "AULA",
      "capacity": 45,
      "status": "ACTIVE"
    }
  ],
  "nextCursor": null
}
~~~

Para traer aulas de una sede, Backoffice indicó:

~~~text
GET /api/v1/backoffice/locations?siteId={siteId}&type=AULA
~~~

La integración debe recorrer páginas usando nextCursor si Backoffice devuelve un cursor. Por ahora este servicio no hace llamadas HTTP a esos endpoints.
La ruta de consulta de sedes se dejó como /api/v1/backoffice/sites por convención y debe confirmarse con ese grupo; la ruta de ubicaciones sí fue provista explícitamente.

## Correspondencia de entidades

Se conservan las claves numéricas locales que ya usan Cursos, Horarios y Asignaciones. Los UUID de Backoffice se guardan como referencias externas únicas:

| Backoffice | Gestión Académica | Uso |
| --- | --- | --- |
| siteId: UUID | Sede.backofficeSiteId: UUID | Identificador externo estable de la sede. |
| name: String | Sede.nombre: String | Nombre de la sede. |
| address: String | Sede.direccion: String | Dirección de la sede. |
| status: String | Sede.estado: String | Estado reportado por Backoffice, por ejemplo ACTIVE. |
| locationId: UUID | Aula.backofficeLocationId: UUID | Identificador externo estable de la ubicación. |
| siteId: UUID | Aula.sede.backofficeSiteId: UUID | Relación con la sede externa; localmente se resuelve a Aula.sede. |
| name: String | Aula.nombre: String | Nombre visible de la ubicación. codigo se conserva para compatibilidad con el modelo local existente. |
| type: AULA/AUDITORIO/COMEDOR/BIBLIOTECA | Aula.tipo: TipoUbicacion | Categoría de ubicación. Sólo las ubicaciones de tipo AULA se pueden asignar a cursos o materias. |
| capacity: number | Aula.capacidadMaxima: int | Capacidad de la ubicación. |
| status: String | Aula.estado: String | Estado reportado por Backoffice. |

La entidad Sede mantiene su id: Long como clave primaria local. Por eso Curso.sedeId sigue siendo el ID local; al sincronizar cursos y sedes, el servicio debe resolver el siteId externo y asociar la sede local correspondiente.

## Configuración preparada

En src/main/resources/application.properties quedaron comentadas estas propiedades:

~~~properties
# backoffice.api.base-url=${BACKOFFICE_API_BASE_URL:http://localhost:8082}
# backoffice.api.sites-path=/api/v1/backoffice/sites
# backoffice.api.locations-path=/api/v1/backoffice/locations
~~~

Cuando se implemente el cliente HTTP se puede habilitar la URL por variable BACKOFFICE_API_BASE_URL. La URL real de ambiente todavía debe ser provista por el grupo de Backoffice.

## Actualización de bases existentes

El esquema de Docker para volúmenes nuevos ya incluye estas columnas. Para una base existente, aplicar manualmente docker/migrations/02-backoffice-sites-and-locations.sql. La migración conserva los registros y completa Aula.nombre con el codigo actual.

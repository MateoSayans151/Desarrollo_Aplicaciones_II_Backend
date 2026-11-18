package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import java.util.List;
import java.util.UUID;

/** Respuestas de /internal/v1/sites y /internal/v1/locations tal como las publica Backoffice. */
public final class BackofficeDto {
    private BackofficeDto() { }

    public record Pagina<T>(List<T> items, String nextCursor) { }

    /** Se leen sólo los campos que usa este módulo; el resto se ignora. */
    public record SedeBackoffice(UUID siteId, String code, String name, String address, String city,
                                 String status) { }

    public record UbicacionBackoffice(UUID locationId, UUID siteId, String code, String name, String type,
                                      Integer capacity, String status) { }

    /** Payload de backoffice.location.upserted.v1. {@code type} es opcional (AULA, LABORATORIO...). */
    public record UbicacionActualizada(UUID locationId, UUID siteId, String name, Integer capacity,
                                       String status, String type) { }

    public record ResumenSincronizacion(int sedesCreadas, int sedesActualizadas, int sedesDesactivadas,
                                        int aulasCreadas, int aulasActualizadas, int aulasDesactivadas,
                                        int aulasDescartadas) { }
}

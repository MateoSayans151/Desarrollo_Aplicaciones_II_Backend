package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import ar.edu.universidad.gestionacademica.Entidades.Sede;
import ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Repositorios.AulaRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.SedeRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.*;

/**
 * Mantiene la copia local del catálogo de Backoffice. Las sedes y aulas se identifican por
 * siteId y locationId; la clave Long local se conserva porque la usan cursos y reservas.
 * Lo que Backoffice deja de informar se marca INACTIVE: nunca se borra, para no perder
 * la historia de cursos y reservas que lo referencian.
 */
@Service
public class SincronizacionBackofficeServicio {
    static final String ACTIVA = "ACTIVE";
    static final String INACTIVA = "INACTIVE";
    private static final Logger log = LoggerFactory.getLogger(SincronizacionBackofficeServicio.class);

    private final PropiedadesBackoffice propiedades;
    private final ClienteBackoffice cliente;
    private final SedeRepositorio sedes;
    private final AulaRepositorio aulas;
    private final TransactionTemplate transaccion;
    private final ReentrantLock enCurso = new ReentrantLock();

    public SincronizacionBackofficeServicio(PropiedadesBackoffice propiedades, ClienteBackoffice cliente,
                                            SedeRepositorio sedes, AulaRepositorio aulas,
                                            TransactionTemplate transaccion) {
        this.propiedades = propiedades;
        this.cliente = cliente;
        this.sedes = sedes;
        this.aulas = aulas;
        this.transaccion = transaccion;
    }

    /** Lee todo el catálogo de Backoffice y lo aplica en una única transacción. */
    public ResumenSincronizacion sincronizar() {
        if (!propiedades.enabled()) {
            throw new ReglaNegocioException("La integración con Backoffice está deshabilitada (BACKOFFICE_ENABLED=false)");
        }
        if (!enCurso.tryLock()) {
            throw new ReglaNegocioException("Ya hay una sincronización con Backoffice en curso");
        }
        try {
            String correlationId = UUID.randomUUID().toString();
            // Primero se lee todo por HTTP, fuera de la transacción: si Backoffice falla a
            // mitad de camino no queda aplicado un catálogo parcial.
            List<SedeBackoffice> sitios = cliente.listarSedes(correlationId);
            Map<UUID, List<UbicacionBackoffice>> aulasPorSede = new LinkedHashMap<>();
            for (SedeBackoffice sitio : sitios) {
                if (sitio.siteId() != null) {
                    aulasPorSede.put(sitio.siteId(), cliente.listarAulas(sitio.siteId(), correlationId));
                }
            }
            ResumenSincronizacion resumen = transaccion.execute(estado -> aplicarCatalogo(sitios, aulasPorSede));
            log.info("Catálogo de Backoffice sincronizado (correlationId {}): {}", correlationId, resumen);
            return resumen;
        } finally {
            enCurso.unlock();
        }
    }

    /** Para tareas en segundo plano: registra el error en vez de propagarlo. */
    public void sincronizarSinPropagar() {
        try {
            sincronizar();
        } catch (RuntimeException ex) {
            log.warn("No se pudo sincronizar el catálogo de Backoffice: {}", ex.getMessage());
        }
    }

    ResumenSincronizacion aplicarCatalogo(List<SedeBackoffice> sitios,
                                          Map<UUID, List<UbicacionBackoffice>> aulasPorSede) {
        Contador contador = new Contador();
        Set<UUID> sitiosInformados = new HashSet<>();
        for (SedeBackoffice sitio : sitios) {
            if (sitio.siteId() == null) continue;
            sitiosInformados.add(sitio.siteId());
            Sede sede = aplicarSede(sitio, contador);
            aplicarAulas(sede, aulasPorSede.getOrDefault(sitio.siteId(), List.of()), contador);
        }
        for (Sede sede : sedes.findByBackofficeSiteIdIsNotNull()) {
            if (!sitiosInformados.contains(sede.getBackofficeSiteId()) && !INACTIVA.equals(sede.getEstado())) {
                sede.setEstado(INACTIVA);
                contador.sedesDesactivadas++;
            }
        }
        return contador.resumen();
    }

    /**
     * Aplica backoffice.location.upserted.v1. Sólo actualiza aulas ya conocidas: el evento no
     * trae el código del espacio, así que un aula nueva se trae con una sincronización completa.
     *
     * @return false si el locationId todavía no está en la copia local
     */
    public boolean aplicarUbicacion(UbicacionActualizada evento) {
        if (evento.locationId() == null) {
            throw new IllegalArgumentException("El evento no trae locationId");
        }
        Optional<Aula> existente = aulas.findByBackofficeLocationId(evento.locationId());
        if (existente.isEmpty()) return false;
        Aula aula = existente.get();
        if (evento.siteId() != null && !evento.siteId().equals(aula.getSede().getBackofficeSiteId())) {
            sedes.findByBackofficeSiteId(evento.siteId()).ifPresent(aula::setSede);
        }
        if (tieneTexto(evento.name())) aula.setNombre(recortar(evento.name(), 255));
        if (evento.capacity() != null && evento.capacity() > 0) aula.setCapacidadMaxima(evento.capacity());
        if (tieneTexto(evento.status())) aula.setEstado(normalizarEstado(evento.status()));
        if (tieneTexto(evento.type())) aula.setTipo(tipo(evento.type()));
        aulas.save(aula);
        return true;
    }

    private Sede aplicarSede(SedeBackoffice sitio, Contador contador) {
        Optional<Sede> vinculada = sedes.findByBackofficeSiteId(sitio.siteId());
        String nombreBackoffice = primerTexto(sitio.name(), sitio.code(), sitio.siteId().toString());
        Sede sede = vinculada.orElseGet(() -> adoptarSedeLocal(nombreBackoffice).orElseGet(Sede::new));
        boolean nueva = sede.getId() == null;
        sede.setBackofficeSiteId(sitio.siteId());
        sede.setNombre(nombreDisponible(sede, nombreBackoffice, sitio));
        sede.setDireccion(recortar(primerTexto(sitio.address(), sitio.city(), "Sin dirección informada"), 255));
        sede.setEstado(normalizarEstado(sitio.status()));
        Sede guardada = sedes.save(sede);
        if (nueva) contador.sedesCreadas++; else contador.sedesActualizadas++;
        return guardada;
    }

    /** Una sede creada a mano con el mismo nombre se vincula, para conservar sus cursos. */
    private Optional<Sede> adoptarSedeLocal(String nombre) {
        return sedes.findByNombre(recortar(nombre, 255)).filter(sede -> sede.getBackofficeSiteId() == null);
    }

    /** El nombre de sede es único localmente; ante un choque se agrega el código de Backoffice. */
    private String nombreDisponible(Sede sede, String nombre, SedeBackoffice sitio) {
        String deseado = recortar(nombre, 255);
        Optional<Sede> otra = sedes.findByNombre(deseado).filter(s -> !Objects.equals(s.getId(), sede.getId()));
        if (otra.isEmpty()) return deseado;
        return recortar(deseado + " (" + primerTexto(sitio.code(), sitio.siteId().toString()) + ")", 255);
    }

    private void aplicarAulas(Sede sede, List<UbicacionBackoffice> ubicaciones, Contador contador) {
        Set<UUID> informadas = new HashSet<>();
        for (UbicacionBackoffice ubicacion : ubicaciones) {
            if (ubicacion.locationId() == null) continue;
            informadas.add(ubicacion.locationId());
            Optional<Aula> existente = aulas.findByBackofficeLocationId(ubicacion.locationId());
            boolean capacidadValida = ubicacion.capacity() != null && ubicacion.capacity() > 0;
            if (existente.isEmpty() && !capacidadValida) {
                log.warn("Se descarta la ubicación {} de Backoffice: capacidad inválida {}",
                        ubicacion.locationId(), ubicacion.capacity());
                contador.aulasDescartadas++;
                continue;
            }
            Aula aula = existente.orElseGet(Aula::new);
            boolean nueva = aula.getId() == null;
            aula.setBackofficeLocationId(ubicacion.locationId());
            aula.setSede(sede);
            aula.setCodigo(codigoDisponible(sede, aula, ubicacion));
            aula.setNombre(recortar(primerTexto(ubicacion.name(), aula.getCodigo()), 255));
            aula.setTipo(tipo(ubicacion.type()));
            if (capacidadValida) aula.setCapacidadMaxima(ubicacion.capacity());
            aula.setEstado(normalizarEstado(ubicacion.status()));
            aulas.save(aula);
            if (nueva) contador.aulasCreadas++; else contador.aulasActualizadas++;
        }
        for (Aula aula : aulas.findBySedeIdAndBackofficeLocationIdIsNotNull(sede.getId())) {
            if (!informadas.contains(aula.getBackofficeLocationId()) && !INACTIVA.equals(aula.getEstado())) {
                aula.setEstado(INACTIVA);
                contador.aulasDesactivadas++;
            }
        }
    }

    /** El código de aula es único por sede; ante un choque con un aula local se usa el locationId. */
    private String codigoDisponible(Sede sede, Aula aula, UbicacionBackoffice ubicacion) {
        String deseado = recortar(primerTexto(ubicacion.code(), ubicacion.name(),
                ubicacion.locationId().toString()), 30);
        boolean ocupado = aulas.findBySedeIdAndCodigo(sede.getId(), deseado)
                .filter(otra -> !Objects.equals(otra.getId(), aula.getId()))
                .isPresent();
        return ocupado ? recortar(ubicacion.locationId().toString(), 30) : deseado;
    }

    static TipoUbicacion tipo(String valor) {
        if (!tieneTexto(valor)) return TipoUbicacion.AULA;
        try {
            return TipoUbicacion.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TipoUbicacion.OTRO;
        }
    }

    static String normalizarEstado(String estado) {
        return tieneTexto(estado) ? recortar(estado.trim().toUpperCase(Locale.ROOT), 30) : ACTIVA;
    }

    private static String primerTexto(String... valores) {
        for (String valor : valores) {
            if (tieneTexto(valor)) return valor.trim();
        }
        return "";
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static final class Contador {
        int sedesCreadas, sedesActualizadas, sedesDesactivadas;
        int aulasCreadas, aulasActualizadas, aulasDesactivadas, aulasDescartadas;

        ResumenSincronizacion resumen() {
            return new ResumenSincronizacion(sedesCreadas, sedesActualizadas, sedesDesactivadas,
                    aulasCreadas, aulasActualizadas, aulasDesactivadas, aulasDescartadas);
        }
    }
}

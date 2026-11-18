package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import ar.edu.universidad.gestionacademica.Entidades.Sede;
import ar.edu.universidad.gestionacademica.Entidades.TipoUbicacion;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Repositorios.AulaRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.SedeRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SincronizacionBackofficeServicioTest {
    private static final UUID SITE = UUID.fromString("3e9b0a12-55cc-4d77-9a01-6b2f8c4e1d90");
    private static final UUID AULA_305 = UUID.fromString("c41a77b2-0000-0000-0000-000000000305");

    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final ClienteBackoffice cliente = mock(ClienteBackoffice.class);
    private final TransactionTemplate transaccion = mock(TransactionTemplate.class);
    private final AtomicLong ids = new AtomicLong(100);
    private SincronizacionBackofficeServicio servicio;

    @BeforeEach
    void preparar() {
        servicio = new SincronizacionBackofficeServicio(
                new PropiedadesBackoffice(true, "https://bo", null, null, 100, null),
                cliente, sedes, aulas, transaccion);
        when(transaccion.execute(any())).thenAnswer(inv -> ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
        when(sedes.findByBackofficeSiteId(any())).thenReturn(Optional.empty());
        when(sedes.findByNombre(anyString())).thenReturn(Optional.empty());
        when(sedes.findByBackofficeSiteIdIsNotNull()).thenReturn(List.of());
        when(sedes.save(any(Sede.class))).thenAnswer(inv -> {
            Sede sede = inv.getArgument(0);
            if (sede.getId() == null) sede.setId(ids.incrementAndGet());
            return sede;
        });
        when(aulas.findByBackofficeLocationId(any())).thenReturn(Optional.empty());
        when(aulas.findBySedeIdAndCodigo(anyLong(), anyString())).thenReturn(Optional.empty());
        when(aulas.findBySedeIdAndBackofficeLocationIdIsNotNull(anyLong())).thenReturn(List.of());
        when(aulas.save(any(Aula.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creaSedesYAulasNuevasConSusReferenciasDeBackoffice() {
        var resumen = servicio.aplicarCatalogo(
                List.of(new SedeBackoffice(SITE, "CEN", "Sede Centro", "Av. 1", "CABA", "ACTIVE")),
                Map.of(SITE, List.of(new UbicacionBackoffice(AULA_305, SITE, "PIA1254", "Aula 305", "AULA", 45, "maintenance"))));

        assertThat(resumen.sedesCreadas()).isEqualTo(1);
        assertThat(resumen.aulasCreadas()).isEqualTo(1);
        verify(aulas).save(argThat(aula -> AULA_305.equals(aula.getBackofficeLocationId())
                && "PIA1254".equals(aula.getCodigo()) && "Aula 305".equals(aula.getNombre())
                && aula.getCapacidadMaxima() == 45 && aula.getTipo() == TipoUbicacion.AULA
                && "MAINTENANCE".equals(aula.getEstado()) && SITE.equals(aula.getSede().getBackofficeSiteId())));
    }

    @Test
    void vinculaLaSedeLocalConElMismoNombreEnLugarDeDuplicarla() {
        Sede local = Sede.builder().id(7L).nombre("Sede Centro").direccion("vieja").build();
        when(sedes.findByNombre("Sede Centro")).thenReturn(Optional.of(local));

        var resumen = servicio.aplicarCatalogo(
                List.of(new SedeBackoffice(SITE, "CEN", "Sede Centro", "Av. 1", null, "ACTIVE")), Map.of());

        assertThat(resumen.sedesCreadas()).isZero();
        assertThat(resumen.sedesActualizadas()).isEqualTo(1);
        assertThat(local.getBackofficeSiteId()).isEqualTo(SITE);
        assertThat(local.getDireccion()).isEqualTo("Av. 1");
    }

    @Test
    void desactivaLoQueBackofficeYaNoInforma() {
        Sede vinculada = Sede.builder().id(7L).nombre("Sede Centro").direccion("Av. 1").backofficeSiteId(SITE).build();
        Sede eliminada = Sede.builder().id(8L).nombre("Sede Vieja").direccion("x").backofficeSiteId(UUID.randomUUID()).build();
        Aula sinInformar = Aula.builder().id(3L).codigo("OLD").nombre("Vieja").capacidadMaxima(10)
                .backofficeLocationId(UUID.randomUUID()).sede(vinculada).build();
        when(sedes.findByBackofficeSiteId(SITE)).thenReturn(Optional.of(vinculada));
        when(sedes.findByBackofficeSiteIdIsNotNull()).thenReturn(List.of(vinculada, eliminada));
        when(aulas.findBySedeIdAndBackofficeLocationIdIsNotNull(7L)).thenReturn(List.of(sinInformar));

        var resumen = servicio.aplicarCatalogo(
                List.of(new SedeBackoffice(SITE, "CEN", "Sede Centro", "Av. 1", null, "ACTIVE")), Map.of(SITE, List.of()));

        assertThat(sinInformar.getEstado()).isEqualTo("INACTIVE");
        assertThat(eliminada.getEstado()).isEqualTo("INACTIVE");
        assertThat(vinculada.getEstado()).isEqualTo("ACTIVE");
        assertThat(resumen.aulasDesactivadas()).isEqualTo(1);
        assertThat(resumen.sedesDesactivadas()).isEqualTo(1);
    }

    @Test
    void descartaUnaAulaNuevaSinCapacidadValida() {
        var resumen = servicio.aplicarCatalogo(
                List.of(new SedeBackoffice(SITE, "CEN", "Sede Centro", "Av. 1", null, "ACTIVE")),
                Map.of(SITE, List.of(new UbicacionBackoffice(AULA_305, SITE, "PIA1254", "Aula 305", "AULA", 0, "ACTIVE"))));

        assertThat(resumen.aulasDescartadas()).isEqualTo(1);
        verify(aulas, never()).save(any());
    }

    @Test
    void usaElLocationIdComoCodigoSiChocaConUnAulaLocal() {
        when(aulas.findBySedeIdAndCodigo(anyLong(), eq("A-101")))
                .thenReturn(Optional.of(Aula.builder().id(1L).codigo("A-101").build()));

        servicio.aplicarCatalogo(
                List.of(new SedeBackoffice(SITE, "CEN", "Sede Centro", "Av. 1", null, "ACTIVE")),
                Map.of(SITE, List.of(new UbicacionBackoffice(AULA_305, SITE, "A-101", "Aula 101", "AULA", 30, "ACTIVE"))));

        verify(aulas).save(argThat(aula -> aula.getCodigo().equals(AULA_305.toString().substring(0, 30))));
    }

    @Test
    void elEventoActualizaCapacidadNombreYEstadoDeUnAulaConocida() {
        Sede sede = Sede.builder().id(7L).backofficeSiteId(SITE).build();
        Aula aula = Aula.builder().id(3L).codigo("PIA1254").nombre("Aula 305").capacidadMaxima(45)
                .backofficeLocationId(AULA_305).sede(sede).build();
        when(aulas.findByBackofficeLocationId(AULA_305)).thenReturn(Optional.of(aula));

        boolean aplicado = servicio.aplicarUbicacion(new UbicacionActualizada(AULA_305, SITE, "Aula 305 bis", 60, "INACTIVE", null));

        assertThat(aplicado).isTrue();
        assertThat(aula.getNombre()).isEqualTo("Aula 305 bis");
        assertThat(aula.getCapacidadMaxima()).isEqualTo(60);
        assertThat(aula.getEstado()).isEqualTo("INACTIVE");
    }

    @Test
    void elEventoDeUnaUbicacionDesconocidaNoCreaNada() {
        assertThat(servicio.aplicarUbicacion(new UbicacionActualizada(AULA_305, SITE, "x", 10, "ACTIVE", null))).isFalse();
        verify(aulas, never()).save(any());
    }

    @Test
    void conLaIntegracionApagadaNoSincroniza() {
        var apagado = new SincronizacionBackofficeServicio(new PropiedadesBackoffice(false, null, null, null, 0, null),
                cliente, sedes, aulas, transaccion);

        assertThatThrownBy(apagado::sincronizar).isInstanceOf(ReglaNegocioException.class);
        verifyNoInteractions(cliente);
    }

    @Test
    void siBackofficeFallaNoAplicaNada() {
        when(cliente.listarSedes(anyString())).thenReturn(List.of(new SedeBackoffice(SITE, "CEN", "Sede", "Av", null, "ACTIVE")));
        when(cliente.listarAulas(eq(SITE), anyString()))
                .thenThrow(new ar.edu.universidad.gestionacademica.Excepciones.IntegracionExternaException("caído"));

        assertThatThrownBy(servicio::sincronizar).hasMessage("caído");
        verifyNoInteractions(transaccion);
    }
}

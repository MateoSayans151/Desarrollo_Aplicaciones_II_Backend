package ar.edu.universidad.gestionacademica.Integracion.Backoffice;

import ar.edu.universidad.gestionacademica.Integracion.Core.PropiedadesServicioCore;
import ar.edu.universidad.gestionacademica.Integracion.Core.ProveedorTokenServicio;
import ar.edu.universidad.gestionacademica.Repositorios.AulaRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.SedeRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Levanta sólo los beans de integración para comprobar el binding de propiedades y los condicionales. */
class ConfiguracionBackofficeTest {
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(ConfiguracionBackoffice.class, ClienteBackoffice.class,
                    ProveedorTokenServicio.class, SincronizacionBackofficeServicio.class)
            .withBean(SedeRepositorio.class, () -> mock(SedeRepositorio.class))
            .withBean(AulaRepositorio.class, () -> mock(AulaRepositorio.class))
            .withBean(TransactionTemplate.class, () -> mock(TransactionTemplate.class));

    @Test
    void conValoresPorDefectoQuedaApagadaYSinTareaProgramada() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PropiedadesBackoffice.class).enabled()).isFalse();
            assertThat(ctx).doesNotHaveBean(ConfiguracionBackoffice.SincronizacionPeriodica.class);
        });
    }

    @Test
    void enlazaLasPropiedadesDeRailway() {
        contexto.withPropertyValues(
                        "backoffice.enabled=true",
                        "backoffice.base-url=https://backend-production-e464.up.railway.app",
                        "backoffice.page-size=50",
                        "backoffice.sync-interval=PT6H",
                        "backoffice.sync-initial-delay=PT1H",
                        "backoffice.events.queue=academic.locations.v1",
                        "core.service.client-id=academic-service",
                        "core.service.token=token-simulado")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    PropiedadesBackoffice propiedades = ctx.getBean(PropiedadesBackoffice.class);
                    assertThat(propiedades.baseUrl()).isEqualTo("https://backend-production-e464.up.railway.app");
                    assertThat(propiedades.sitesPath()).isEqualTo("/internal/v1/sites");
                    assertThat(propiedades.pageSize()).isEqualTo(50);
                    assertThat(propiedades.events().queue()).isEqualTo("academic.locations.v1");
                    assertThat(ctx.getBean(PropiedadesServicioCore.class).clientId()).isEqualTo("academic-service");
                    assertThat(ctx.getBean(ProveedorTokenServicio.class).token()).isEqualTo("token-simulado");
                    assertThat(ctx).hasSingleBean(ConfiguracionBackoffice.SincronizacionPeriodica.class);
                });
    }
}

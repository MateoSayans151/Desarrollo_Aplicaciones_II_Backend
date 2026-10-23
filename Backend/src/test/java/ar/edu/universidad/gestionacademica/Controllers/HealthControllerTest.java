package ar.edu.universidad.gestionacademica.Controllers;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {
    private final DataSource dataSource = mock(DataSource.class);
    private final HealthController controller = new HealthController(dataSource);

    @Test
    void readinessEstaArribaCuandoLaBaseResponde() throws Exception {
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        var respuesta = controller.ready();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).containsEntry("status", "UP");
    }

    @Test
    void readinessEstaAbajoCuandoNoHayConexion() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("sin conexion"));

        var respuesta = controller.ready();

        assertThat(respuesta.getStatusCode().value()).isEqualTo(503);
        assertThat(respuesta.getBody()).containsEntry("status", "DOWN");
    }
}

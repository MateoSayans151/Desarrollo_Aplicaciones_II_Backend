package ar.edu.universidad.gestionacademica.Configuracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Informa contra que base quedo conectada la aplicacion al terminar de arrancar.
 * Sirve para detectar el caso silencioso: si el .env no se lee, DB_URL toma su
 * valor por defecto y la app funciona igual pero contra H2 en memoria.
 */
@Component
public class InformeConexionBaseDatos {
    private static final Logger log = LoggerFactory.getLogger(InformeConexionBaseDatos.class);

    private final DataSource dataSource;

    public InformeConexionBaseDatos(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void informarConexion() {
        try (Connection conexion = dataSource.getConnection()) {
            DatabaseMetaData meta = conexion.getMetaData();
            String motor = meta.getDatabaseProductName();
            String url = meta.getURL();

            log.info("Base de datos: {} {}", motor, meta.getDatabaseProductVersion());
            log.info("URL JDBC     : {}", ocultarCredenciales(url));
            log.info("Usuario      : {}", meta.getUserName());

            if (motor != null && motor.toLowerCase().contains("h2")) {
                log.warn("=================================================================");
                log.warn("ATENCION: estas conectado a H2 EN MEMORIA, no a PostgreSQL.");
                log.warn("Los datos se pierden al apagar la aplicacion.");
                log.warn("Revisa que el archivo .env exista y defina DB_URL, DB_USERNAME");
                log.warn("y DB_PASSWORD, y que VS Code este abierto en la carpeta Backend.");
                log.warn("=================================================================");
            } else {
                log.info("Conexion a PostgreSQL establecida correctamente.");
            }
        } catch (Exception ex) {
            log.error("No se pudo verificar la conexion a la base de datos.", ex);
        }
    }

    /** Evita que una clave incrustada en la URL termine escrita en los logs. */
    private String ocultarCredenciales(String url) {
        if (url == null) {
            return "(desconocida)";
        }
        return url.replaceAll("(?i)(password=)[^&]*", "$1****");
    }
}

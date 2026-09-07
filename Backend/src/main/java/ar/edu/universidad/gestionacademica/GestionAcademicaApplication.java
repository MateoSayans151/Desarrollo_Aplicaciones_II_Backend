package ar.edu.universidad.gestionacademica;

import me.paulschwarz.springdotenv.spring.DotenvApplicationInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class GestionAcademicaApplication {
    public static void main(String[] args) {
        // El jar de spring-dotenv no trae META-INF/spring.factories, asi que no se
        // autoregistra: hay que sumar el initializer a mano para que lea el .env
        // antes de que Spring intente resolver DB_URL/DB_USERNAME/DB_PASSWORD.
        new SpringApplicationBuilder(GestionAcademicaApplication.class)
                .initializers(new DotenvApplicationInitializer())
                .run(args);
    }
}

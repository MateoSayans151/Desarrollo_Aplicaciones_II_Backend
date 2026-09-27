package ar.edu.universidad.gestionacademica.Configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionOpenApi {
    private static final String SEGURIDAD_BEARER = "coreBearer";

    @Bean
    OpenAPI gestionAcademicaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestion Academica y Planificacion")
                        .description("Administracion de carreras, planes, asignaturas, aulas, periodos y regularidad.")
                        .version("1.0.0")
                        .contact(new Contact().name("Secretaria Academica")))
                .addSecurityItem(new SecurityRequirement().addList(SEGURIDAD_BEARER))
                .components(new Components().addSecuritySchemes(
                        SEGURIDAD_BEARER,
                        new SecurityScheme()
                                .name(SEGURIDAD_BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

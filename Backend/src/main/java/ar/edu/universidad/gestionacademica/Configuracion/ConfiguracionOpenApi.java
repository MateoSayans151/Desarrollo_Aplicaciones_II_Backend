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
    private static final String SEGURIDAD_BASIC = "autenticacionBasic";

    @Bean
    OpenAPI gestionAcademicaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestion Academica y Planificacion")
                        .description("Administracion de carreras, planes, asignaturas, aulas, periodos y regularidad.")
                        .version("1.0.0")
                        .contact(new Contact().name("Secretaria Academica")))
                .addSecurityItem(new SecurityRequirement().addList(SEGURIDAD_BASIC))
                .components(new Components().addSecuritySchemes(
                        SEGURIDAD_BASIC,
                        new SecurityScheme()
                                .name(SEGURIDAD_BASIC)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}

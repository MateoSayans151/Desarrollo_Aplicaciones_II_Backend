package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import ar.edu.universidad.gestionacademica.Repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/usuarios/login", "/api/usuarios/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academica/**", "/api/planificacion/**")
                                .hasAnyRole("ADMINISTRATIVO", "USUARIO")
                        .anyRequest().hasRole("ADMINISTRATIVO"))
                .httpBasic(basic -> {})
                .build();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    CommandLineRunner crearAdministradorInicial(
            UsuarioRepositorio usuarios,
            PasswordEncoder encoder,
            @Value("${universidad.admin.email}") String email,
            @Value("${universidad.admin.password}") String clave,
            @Value("${universidad.dominio-email}") String dominio) {
        if (!email.toLowerCase().endsWith("@" + dominio.toLowerCase())) {
            throw new IllegalStateException("El administrador debe utilizar un mail del dominio universitario configurado");
        }
        return args -> {
            if (!usuarios.existsByEmailIgnoreCase(email)) {
                usuarios.save(Usuario.builder()
                        .nombre("Administrador academico")
                        .email(email.toLowerCase())
                        .password(encoder.encode(clave))
                        .permiso(Usuario.Permiso.ADMINISTRATIVO)
                        .build());
            }
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

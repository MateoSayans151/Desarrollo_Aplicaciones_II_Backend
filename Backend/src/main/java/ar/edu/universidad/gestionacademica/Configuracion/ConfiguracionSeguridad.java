package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import ar.edu.universidad.gestionacademica.Repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain filtros(HttpSecurity http, CorsConfigurationSource origenesCors) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(origenesCors))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/usuarios/login", "/api/usuarios/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academica/**", "/api/planificacion/**")
                                .hasAnyRole("ADMINISTRATIVO", "USUARIO")
                        .anyRequest().hasRole("ADMINISTRATIVO"))
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * El frontend usa cookies de sesion (no un token Bearer), asi que el origen
     * permitido no puede ser "*": tiene que ser explicito para poder mandar
     * Access-Control-Allow-Credentials junto con el origen real.
     */
    @Bean
    CorsConfigurationSource origenesCors(@Value("${cors.allowed-origins:http://localhost:5173}") String origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        configuracion.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
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

    /**
     * Usuario fijo que el frontend usa para decidir el modo demo: si el que
     * inicia sesion es este, la app muestra los mocks locales en vez de pedir
     * datos reales al backend (ver app/modoDemo.ts en el frontend).
     */
    @Bean
    CommandLineRunner crearUsuarioDemo(
            UsuarioRepositorio usuarios,
            PasswordEncoder encoder,
            @Value("${universidad.demo.email:demo@universidad.edu.ar}") String email,
            @Value("${universidad.demo.password:DemoMocks123!}") String clave) {
        return args -> {
            if (!usuarios.existsByEmailIgnoreCase(email)) {
                usuarios.save(Usuario.builder()
                        .nombre("Usuario demo")
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

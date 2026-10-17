package ar.edu.universidad.gestionacademica.Configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ConfiguracionSeguridad {
    /**
     * La sesión local emite ROLE_ADMINISTRATIVO, mientras que CORE puede enviar
     * el nombre del rol con o sin el prefijo ROLE_. Se aceptan ambas variantes
     * durante la transición de autenticación.
     */
    private static final String[] AUTORIDADES_ADMINISTRATIVAS = {
            "ROLE_ADMINISTRATIVO", "ADMINISTRATIVO",
            "ROLE_ADMINISTRADOR", "ADMINISTRADOR",
            "ROLE_ACADEMIC_ADMIN", "ACADEMIC_ADMIN"
    };

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, CorsConfigurationSource origenesCors) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(origenesCors))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/health/live", "/health/ready", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/academica/**", "/api/planificacion/**")
                                .hasAnyAuthority(AUTORIDADES_ADMINISTRATIVAS)
                        .requestMatchers("/api/**").hasAnyAuthority(AUTORIDADES_ADMINISTRATIVAS)
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService usuarios, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarios);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource origenesCors(@Value("${cors.allowed-origins:http://localhost:5173}") String origenesPermitidos) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.asList(origenesPermitidos.split(",")));
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Correlation-Id"));
        configuracion.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}

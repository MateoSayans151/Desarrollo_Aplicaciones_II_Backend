package ar.edu.universidad.gestionacademica.Configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/academica/**", "/api/planificacion/**")
                                .hasAnyAuthority(AUTORIDADES_ADMINISTRATIVAS)
                        .requestMatchers("/api/**").hasAnyAuthority(AUTORIDADES_ADMINISTRATIVAS)
                        .anyRequest().denyAll())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((request, response, ex) -> escribirProblema(request, response,
                                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Iniciá sesión para continuar"))
                        .accessDeniedHandler((request, response, ex) -> escribirProblema(request, response,
                                HttpStatus.FORBIDDEN, "FORBIDDEN", "Tu cuenta no tiene permiso para realizar esta acción")))
                .build();
    }

    /** Los rechazos de Spring Security usan la misma forma que ManejadorErrores. */
    private void escribirProblema(HttpServletRequest request, HttpServletResponse response, HttpStatus estado,
                                  String codigo, String detalle) throws IOException {
        Map<String, Object> problema = new LinkedHashMap<>();
        problema.put("type", "https://uadenet.local/errors/" + codigo.toLowerCase());
        problema.put("title", estado.getReasonPhrase());
        problema.put("status", estado.value());
        problema.put("detail", detalle);
        problema.put("instance", request.getRequestURI());
        problema.put("code", codigo);
        String recibido = request.getHeader("X-Correlation-Id");
        problema.put("correlationId", recibido == null || recibido.isBlank() ? UUID.randomUUID().toString() : recibido);
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(new ObjectMapper().writeValueAsString(problema));
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

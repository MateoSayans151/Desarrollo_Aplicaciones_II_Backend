package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Seguridad.AccesoAcademico;
import ar.edu.universidad.gestionacademica.Seguridad.ConvertidorJwtCore;
import ar.edu.universidad.gestionacademica.Seguridad.CorrelationId;
import ar.edu.universidad.gestionacademica.Seguridad.FiltroFirmaCore;
import ar.edu.universidad.gestionacademica.Seguridad.RespuestaProblema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Dos formas de autenticarse conviven durante la transición:
 * <ul>
 *   <li>JWT de CORE (Bearer), que llega por el gateway a /api/v1/** o directo a /internal/v1/**;</li>
 *   <li>la sesión local (JSESSIONID) que usa el frontend propio hasta migrar al login de CORE.</li>
 * </ul>
 */
@Configuration
public class ConfiguracionSeguridad {
    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSeguridad.class);
    private static final String CURSOS = "/api/v1/planificacion/cursos";

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, CorsConfigurationSource origenesCors, AccesoAcademico acceso,
                                JwtDecoder decodificadorJwtCore,
                                @Value("${core.gateway.signature-secret:}") String secretoFirma,
                                @Value("${core.gateway.signature-tolerance:PT5M}") String toleranciaFirma)
            throws Exception {
        FiltroFirmaCore firma = new FiltroFirmaCore(secretoFirma, Duration.parse(toleranciaFirma));
        if (!firma.activo()) {
            log.warn("CORE_GATEWAY_SIGNATURE_SECRET no está configurado: no se valida X-Core-Signature");
        }
        AuthenticationEntryPoint noAutenticado = (request, response, ex) -> RespuestaProblema.escribir(
                request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                ex instanceof OAuth2AuthenticationException
                        ? "El token de CORE no es válido o venció"
                        : "Iniciá sesión para continuar");
        AccessDeniedHandler sinPermiso = (request, response, ex) -> RespuestaProblema.escribir(
                request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Tu cuenta no tiene permiso para realizar esta acción");

        return http
                .cors(cors -> cors.configurationSource(origenesCors))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/health/live", "/health/ready", "/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                        // Backend a backend: sólo tokens de servicio.
                        .requestMatchers(HttpMethod.GET, "/internal/v1/**").access(acceso.servicioInterno())
                        // Datos personales de un curso: administración o el docente que lo dicta.
                        .requestMatchers(HttpMethod.GET, CURSOS + "/{cursoId}/notas/{alumnoId}")
                                .access(acceso.administrativoDocenteOAlumnoPropio())
                        .requestMatchers(HttpMethod.GET, CURSOS + "/{cursoId}", CURSOS + "/{cursoId}/docentes",
                                CURSOS + "/{cursoId}/alumnos", CURSOS + "/{cursoId}/notas")
                                .access(acceso.administrativoODocenteDelCurso())
                        .requestMatchers(HttpMethod.PUT, CURSOS + "/{cursoId}/notas/{alumnoId}",
                                CURSOS + "/{cursoId}/inscripciones/{alumnoId}", "/api/v1/regularidad/cursos/{cursoId}")
                                .access(acceso.administrativoODocenteDelCurso())
                        // Autoinscripción: el controlador valida que el alumnoId del body sea el sub.
                        .requestMatchers(HttpMethod.POST, CURSOS + "/{cursoId}/inscripciones")
                                .access(acceso.administrativoOAlumno())
                        .requestMatchers(HttpMethod.GET, "/api/v1/planificacion/alumnos/{alumnoId}/inscripciones")
                                .access(acceso.administrativoOAlumnoPropio())
                        // Catálogos que leen los portales.
                        .requestMatchers(HttpMethod.GET, "/api/v1/academica/**", "/api/v1/planificacion/periodos",
                                "/api/v1/planificacion/turnos-examen", CURSOS, "/api/v1/planificacion/sedes/**",
                                "/api/v1/planificacion/aulas/*/agenda", "/api/v1/planificacion/asignaturas/*/agenda",
                                "/api/v1/regularidad/cursos/*")
                                .access(acceso.lector())
                        // Todo lo demás es administración.
                        .requestMatchers("/api/v1/**").access(acceso.administrativo())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.decoder(decodificadorJwtCore).jwtAuthenticationConverter(new ConvertidorJwtCore()))
                        .authenticationEntryPoint(noAutenticado)
                        .accessDeniedHandler(sinPermiso))
                .addFilterBefore(firma, BearerTokenAuthenticationFilter.class)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(noAutenticado)
                        .accessDeniedHandler(sinPermiso))
                .build();
    }

    /** RS256 con la clave del JWKS de CORE (por kid), iss y aud=uadenet. */
    @Bean
    JwtDecoder decodificadorJwtCore(@Value("${core.jwt.jwk-set-uri:}") String jwks,
                                    @Value("${core.jwt.issuer:}") String emisor,
                                    @Value("${core.jwt.audience:}") String audiencia) {
        if (jwks.isBlank()) {
            log.warn("CORE_JWT_JWK_SET_URI no está configurado: se rechaza todo token Bearer");
            return token -> { throw new BadJwtException("La validación de JWT de CORE no está configurada"); };
        }
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withJwkSetUri(jwks)
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> base = emisor.isBlank()
                ? JwtValidators.createDefault() : JwtValidators.createDefaultWithIssuer(emisor);
        OAuth2TokenValidator<Jwt> validador = audiencia.isBlank() ? base : new DelegatingOAuth2TokenValidator<>(base,
                new JwtClaimValidator<Collection<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(audiencia)));
        decodificador.setJwtValidator(validador);
        return decodificador;
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
        configuracion.setAllowedOrigins(Arrays.stream(origenesPermitidos.split(","))
                .map(String::trim)
                // El navegador manda el Origin sin barra final: https://app.vercel.app/ no coincidiría.
                .map(origen -> origen.endsWith("/") ? origen.substring(0, origen.length() - 1) : origen)
                .filter(origen -> !origen.isEmpty()).toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Content-Type", "Authorization", CorrelationId.HEADER));
        configuracion.setExposedHeaders(List.of(CorrelationId.HEADER));
        configuracion.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}

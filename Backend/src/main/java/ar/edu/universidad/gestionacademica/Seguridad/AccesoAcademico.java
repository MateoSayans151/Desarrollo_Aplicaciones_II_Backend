package ar.edu.universidad.gestionacademica.Seguridad;

import ar.edu.universidad.gestionacademica.Repositorios.CursoDocenteRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Reglas de autorización acordadas con CORE:
 * <ul>
 *   <li>Escritura administrativa: ACADEMIC_ADMIN o academic:manage (y el ADMINISTRATIVO de la sesión local).</li>
 *   <li>STUDENT y TEACHER no traen permisos académicos: se valida que el recurso sea suyo.
 *       El alumno es el mismo {@code sub}; el docente tiene que estar asignado al curso.</li>
 *   <li>/internal/v1/** sólo admite tokens de servicio (claim {@code service}).</li>
 * </ul>
 */
@Component
public class AccesoAcademico {
    static final Set<String> ADMINISTRATIVAS = Set.of(
            "ROLE_ADMINISTRATIVO", "ADMINISTRATIVO",
            "ROLE_ADMINISTRADOR", "ADMINISTRADOR",
            "ROLE_ACADEMIC_ADMIN", "ACADEMIC_ADMIN",
            "academic:manage");
    static final Set<String> ALUMNO = Set.of("STUDENT", "ROLE_STUDENT");
    static final Set<String> DOCENTE = Set.of("TEACHER", "ROLE_TEACHER");

    private final CursoDocenteRepositorio docentes;
    private final Set<String> serviciosPermitidos;

    public AccesoAcademico(CursoDocenteRepositorio docentes,
                           @Value("${core.internal.allowed-services:}") String serviciosPermitidos) {
        this.docentes = docentes;
        this.serviciosPermitidos = Arrays.stream(serviciosPermitidos.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public AuthorizationManager<RequestAuthorizationContext> administrativo() {
        return regla((auth, ctx) -> esAdministrativo(auth));
    }

    /** Lectura de catálogos para portales: administración, alumnos y docentes. */
    public AuthorizationManager<RequestAuthorizationContext> lector() {
        return regla((auth, ctx) -> esAdministrativo(auth) || tiene(auth, ALUMNO) || tiene(auth, DOCENTE));
    }

    public AuthorizationManager<RequestAuthorizationContext> administrativoOAlumno() {
        return regla((auth, ctx) -> esAdministrativo(auth) || tiene(auth, ALUMNO));
    }

    public AuthorizationManager<RequestAuthorizationContext> administrativoODocenteDelCurso() {
        return regla((auth, ctx) -> esAdministrativo(auth) || dictaElCurso(auth, ctx.getVariables().get("cursoId")));
    }

    public AuthorizationManager<RequestAuthorizationContext> administrativoOAlumnoPropio() {
        return regla((auth, ctx) -> esAdministrativo(auth) || esElAlumno(auth, ctx.getVariables().get("alumnoId")));
    }

    public AuthorizationManager<RequestAuthorizationContext> administrativoDocenteOAlumnoPropio() {
        return regla((auth, ctx) -> esAdministrativo(auth)
                || esElAlumno(auth, ctx.getVariables().get("alumnoId"))
                || dictaElCurso(auth, ctx.getVariables().get("cursoId")));
    }

    public AuthorizationManager<RequestAuthorizationContext> servicioInterno() {
        return regla((auth, ctx) -> {
            String servicio = servicio(auth);
            return servicio != null && (serviciosPermitidos.isEmpty() || serviciosPermitidos.contains(servicio));
        });
    }

    /** Para el body de una inscripción: un alumno sólo puede inscribirse a sí mismo. */
    public void validarAlumnoPropio(String alumnoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!esAdministrativo(auth) && !esElAlumno(auth, alumnoId)) {
            throw new AccessDeniedException("Un alumno sólo puede inscribirse a sí mismo");
        }
    }

    public boolean esAdministrativo(Authentication auth) {
        return tiene(auth, ADMINISTRATIVAS);
    }

    private boolean esElAlumno(Authentication auth, String alumnoId) {
        String sujeto = sujetoCore(auth);
        return tiene(auth, ALUMNO) && sujeto != null && sujeto.equals(alumnoId);
    }

    private boolean dictaElCurso(Authentication auth, String cursoId) {
        String sujeto = sujetoCore(auth);
        if (!tiene(auth, DOCENTE) || sujeto == null || cursoId == null) return false;
        try {
            return docentes.existsByCursoIdAndDocenteId(Long.valueOf(cursoId), sujeto);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    /** Sólo los usuarios de CORE tienen un sub que coincide con alumnoId y docenteId. */
    private static String sujetoCore(Authentication auth) {
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject() : null;
    }

    private static String servicio(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwt)) return null;
        String servicio = jwt.getToken().getClaimAsString("service");
        return servicio == null || servicio.isBlank() ? null : servicio;
    }

    private static boolean tiene(Authentication auth, Set<String> autoridades) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(autoridades::contains);
    }

    private static AuthorizationManager<RequestAuthorizationContext> regla(
            BiPredicate<Authentication, RequestAuthorizationContext> condicion) {
        return (autenticacion, contexto) -> new AuthorizationDecision(condicion.test(autenticacion.get(), contexto));
    }
}

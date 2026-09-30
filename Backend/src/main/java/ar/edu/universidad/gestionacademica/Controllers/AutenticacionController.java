package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.UsuarioServicio;
import ar.edu.universidad.gestionacademica.Modelos.LoginUsuarioDto;
import ar.edu.universidad.gestionacademica.Modelos.UsuarioDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {
    private final AuthenticationManager authenticationManager;
    private final UsuarioServicio usuarios;
    private final SecurityContextRepository contextoSesion = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public UsuarioDto login(@Valid @RequestBody LoginUsuarioDto dto,
                            HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            contextoSesion.saveContext(contexto, request, response);
            return usuarios.buscarPorEmail(autenticacion.getName());
        } catch (AuthenticationException ex) {
            throw new CredencialesInvalidasException();
        }
    }

    @GetMapping("/me")
    public UsuarioDto actual(Authentication autenticacion) {
        return usuarios.buscarPorEmail(autenticacion.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        SecurityContextHolder.clearContext();
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    private static class CredencialesInvalidasException extends RuntimeException { }
}

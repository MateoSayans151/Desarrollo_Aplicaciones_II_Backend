package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.UsuarioServicio;
import ar.edu.universidad.gestionacademica.Modelos.CrearUsuarioDto;
import ar.edu.universidad.gestionacademica.Modelos.LoginUsuarioDto;
import ar.edu.universidad.gestionacademica.Modelos.UsuarioDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final AuthenticationManager authenticationManager;
    private final UsuarioServicio usuarioServicio;

    @PostMapping("/login")
    public ResponseEntity<UsuarioDto> login(@Valid @RequestBody LoginUsuarioDto datos,
                                             HttpServletRequest request) {
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(datos.email(), datos.password()));

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacion);
            SecurityContextHolder.setContext(contexto);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);

            return ResponseEntity.ok(usuarioServicio.buscarPorEmail(autenticacion.getName()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/actual")
    public UsuarioDto usuarioActual(Authentication autenticacion) {
        return usuarioServicio.buscarPorEmail(autenticacion.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody CrearUsuarioDto datos) {
        return usuarioServicio.crear(datos);
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarioServicio.listar();
    }
}

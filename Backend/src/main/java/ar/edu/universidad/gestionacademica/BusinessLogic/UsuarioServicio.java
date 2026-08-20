package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Modelos.CrearUsuarioDto;
import ar.edu.universidad.gestionacademica.Modelos.UsuarioDto;
import ar.edu.universidad.gestionacademica.Repositorios.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServicio implements UserDetailsService {
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;

    @Value("${universidad.dominio-email}")
    private String dominioUniversidad;

    public UsuarioDto crear(CrearUsuarioDto dto) {
        validarEmailUniversitario(dto.email());
        if (usuarios.existsByEmailIgnoreCase(dto.email())) {
            throw new ReglaNegocioException("Ya existe un usuario con ese email");
        }
        Usuario usuario = usuarios.save(Usuario.builder()
                .nombre(dto.nombre())
                .email(dto.email().toLowerCase())
                .password(passwordEncoder.encode(dto.password()))
                .permiso(dto.permiso())
                .build());
        return respuesta(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDto buscarPorEmail(String email) {
        return respuesta(usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado")));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email o contraseña incorrectos"));
        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuario.getPermiso().name())
                .disabled(!usuario.isActivo())
                .build();
    }

    private void validarEmailUniversitario(String email) {
        if (!email.toLowerCase().endsWith("@" + dominioUniversidad.toLowerCase())) {
            throw new ReglaNegocioException("El usuario debe utilizar un email de la universidad");
        }
    }

    private UsuarioDto respuesta(Usuario usuario) {
        return new UsuarioDto(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
                usuario.getPermiso(), usuario.isActivo());
    }
}

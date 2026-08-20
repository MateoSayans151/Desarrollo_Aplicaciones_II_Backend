package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.Usuario;

/** Informacion publica del usuario. Nunca expone la contrasena ni su hash. */
public record UsuarioDto(Long id, String nombre, String email,
                         Usuario.Permiso permiso, boolean activo) { }

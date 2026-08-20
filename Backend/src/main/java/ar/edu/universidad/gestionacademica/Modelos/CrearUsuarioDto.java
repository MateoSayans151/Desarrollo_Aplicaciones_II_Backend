package ar.edu.universidad.gestionacademica.Modelos;

import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import jakarta.validation.constraints.*;

public record CrearUsuarioDto(@NotBlank String nombre, @Email @NotBlank String email,
                              @NotBlank @Size(min = 8) String password,
                              @NotNull Usuario.Permiso permiso) { }

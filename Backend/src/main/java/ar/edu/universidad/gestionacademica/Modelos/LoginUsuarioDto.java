package ar.edu.universidad.gestionacademica.Modelos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginUsuarioDto(@Email @NotBlank String email, @NotBlank String password) { }

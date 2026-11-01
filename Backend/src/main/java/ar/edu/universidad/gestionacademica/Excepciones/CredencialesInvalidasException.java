package ar.edu.universidad.gestionacademica.Excepciones;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}

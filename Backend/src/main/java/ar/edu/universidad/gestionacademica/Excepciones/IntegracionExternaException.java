package ar.edu.universidad.gestionacademica.Excepciones;

/** Falla al comunicarse con otro módulo (CORE, Backoffice). Se responde como 502. */
public class IntegracionExternaException extends RuntimeException {
    public IntegracionExternaException(String mensaje) {
        super(mensaje);
    }

    public IntegracionExternaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}

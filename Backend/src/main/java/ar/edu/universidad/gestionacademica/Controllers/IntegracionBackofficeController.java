package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.Integracion.Backoffice.SincronizacionBackofficeServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ar.edu.universidad.gestionacademica.Integracion.Backoffice.BackofficeDto.ResumenSincronizacion;

@RestController
@RequestMapping("/api/v1/planificacion/sedes")
@RequiredArgsConstructor
public class IntegracionBackofficeController {
    private final SincronizacionBackofficeServicio sincronizacion;

    /** Fuerza la lectura del catálogo de sedes y aulas de Backoffice sin esperar la tarea periódica. */
    @PostMapping("/sincronizar")
    public ResumenSincronizacion sincronizar() {
        return sincronizacion.sincronizar();
    }
}

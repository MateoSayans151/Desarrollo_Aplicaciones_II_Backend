package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.RegularidadServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.*;

@RestController
@RequestMapping("/api/regularidad")
@RequiredArgsConstructor
public class RegularidadController {
    private final RegularidadServicio servicio;

    @GetMapping("/cursos/{cursoId}")
    public CondicionesRegularidadCursoDto consultar(@PathVariable Long cursoId) {
        return servicio.consultar(cursoId);
    }

    @PutMapping("/cursos/{cursoId}")
    public CondicionesRegularidadCursoDto actualizar(@PathVariable Long cursoId,
                                                      @Valid @RequestBody GuardarCondicionesRegularidadDto datos) {
        return servicio.actualizar(cursoId, datos);
    }
}

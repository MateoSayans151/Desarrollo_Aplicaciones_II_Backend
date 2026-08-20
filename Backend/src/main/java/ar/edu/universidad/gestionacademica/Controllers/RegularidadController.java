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

    @PostMapping("/validar")
    public ResultadoRegularidadDto validar(@Valid @RequestBody ValidarRegularidadDto datos) {
        return servicio.validar(datos);
    }
}

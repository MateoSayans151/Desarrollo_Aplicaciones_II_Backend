package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.EstructuraAcademicaServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.*;

/**
 * Rutas backend a backend para otros módulos (Backoffice en el alta de alumnos). Se llaman
 * directo, sin pasar por el gateway, con el token de servicio de CORE. Devuelven lo mismo
 * que sus equivalentes de /api/v1/academica.
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternoController {
    private final EstructuraAcademicaServicio servicio;

    @GetMapping("/carreras")
    public List<CarreraRespuestaDto> carreras() { return servicio.listarCarreras(); }

    @GetMapping("/carreras/con-ultimo-plan-activo")
    public List<CarreraRespuestaDto> carrerasConUltimoPlanActivo() { return servicio.listarCarreras(); }

    @GetMapping("/carreras/{carreraId}/planes")
    public List<PlanRespuestaDto> planes(@PathVariable Long carreraId) { return servicio.listarPlanes(carreraId); }

    @GetMapping("/planes/{planId}/asignaturas")
    public List<AsignaturaRespuestaDto> asignaturas(@PathVariable Long planId) {
        return servicio.listarAsignaturas(planId);
    }
}

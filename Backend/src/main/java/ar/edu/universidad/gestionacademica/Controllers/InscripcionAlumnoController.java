package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.CursoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.CursoDto.InscripcionAlumnoRespuestaDto;

@RestController
@RequestMapping("/api/planificacion/alumnos")
@RequiredArgsConstructor
public class InscripcionAlumnoController {
    private final CursoServicio servicio;

    @GetMapping("/{alumnoId}/inscripciones")
    public List<InscripcionAlumnoRespuestaDto> listar(@PathVariable String alumnoId,
                                                       @RequestParam(required = false) Long periodoId) {
        return servicio.listarInscripcionesAlumno(alumnoId, periodoId);
    }
}

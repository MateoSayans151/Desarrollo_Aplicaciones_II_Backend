package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.CursoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.CursoDto.*;

@RestController
@RequestMapping("/api/planificacion/cursos")
@RequiredArgsConstructor
public class CursoController {
    private final CursoServicio servicio;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public CursoRespuestaDto crear(@Valid @RequestBody CrearCursoDto dto) { return servicio.crear(dto); }

    @GetMapping
    public List<CursoRespuestaDto> listar(@RequestParam(required = false) Long periodoId) { return servicio.listar(periodoId); }

    @GetMapping("/{cursoId}")
    public CursoDetalleAcademicoRespuestaDto detalle(@PathVariable Long cursoId) { return servicio.detalle(cursoId); }

    @PutMapping("/{cursoId}/estado")
    public CursoRespuestaDto actualizarEstado(@PathVariable Long cursoId,
                                              @Valid @RequestBody ActualizarEstadoCursoDto dto) {
        return servicio.actualizarEstado(cursoId, dto);
    }

    @PostMapping("/{cursoId}/docentes") @ResponseStatus(HttpStatus.CREATED)
    public CursoDocenteRespuestaDto asignarDocente(@PathVariable Long cursoId,
                                                   @Valid @RequestBody CrearCursoDocenteDto dto) {
        return servicio.asignarDocente(cursoId, dto);
    }

    @PostMapping("/{cursoId}/inscripciones") @ResponseStatus(HttpStatus.CREATED)
    public InscripcionCursoRespuestaDto inscribirAlumno(@PathVariable Long cursoId,
                                                        @Valid @RequestBody CrearInscripcionCursoDto dto) {
        return servicio.inscribirAlumno(cursoId, dto);
    }

    @PutMapping("/{cursoId}/inscripciones/{alumnoId}")
    public InscripcionCursoRespuestaDto actualizarInscripcion(@PathVariable Long cursoId, @PathVariable String alumnoId,
                                                              @Valid @RequestBody ActualizarInscripcionCursoDto dto) {
        return servicio.actualizarInscripcion(cursoId, alumnoId, dto);
    }

    @PostMapping("/{cursoId}/horarios") @ResponseStatus(HttpStatus.CREATED)
    public HorarioCursoRespuestaDto agregarHorario(@PathVariable Long cursoId,
                                                   @Valid @RequestBody CrearHorarioCursoDto dto) {
        return servicio.agregarHorario(cursoId, dto);
    }
}

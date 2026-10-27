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

    @GetMapping("/{cursoId}/docentes")
    public List<CursoDocenteRespuestaDto> listarDocentes(@PathVariable Long cursoId) {
        return servicio.listarDocentes(cursoId);
    }

    @GetMapping("/{cursoId}/alumnos")
    public List<InscripcionCursoRespuestaDto> listarAlumnos(@PathVariable Long cursoId) {
        return servicio.listarAlumnos(cursoId);
    }

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

    @GetMapping("/{cursoId}/notas")
    public List<NotaFinalCursoRespuestaDto> listarNotas(@PathVariable Long cursoId) {
        return servicio.listarNotas(cursoId);
    }

    @GetMapping("/{cursoId}/notas/{alumnoId}")
    public NotaFinalCursoRespuestaDto consultarNota(@PathVariable Long cursoId, @PathVariable String alumnoId) {
        return servicio.consultarNota(cursoId, alumnoId);
    }

    @PutMapping("/{cursoId}/notas/{alumnoId}")
    public NotaFinalCursoRespuestaDto cargarNota(@PathVariable Long cursoId, @PathVariable String alumnoId,
                                                 @Valid @RequestBody CargarNotaFinalDto dto) {
        return servicio.cargarNota(cursoId, alumnoId, dto);
    }

    @PutMapping("/{cursoId}/horario")
    public CursoRespuestaDto actualizarHorario(@PathVariable Long cursoId,
                                               @Valid @RequestBody ActualizarHorarioCursoDto dto) {
        return servicio.actualizarHorario(cursoId, dto);
    }
}

package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.PlanificacionServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.PlanificacionDto.*;

@RestController
@RequestMapping("/api/v1/planificacion")
@RequiredArgsConstructor
public class PlanificacionController {
    private final PlanificacionServicio servicio;

    @PostMapping("/sedes") @ResponseStatus(HttpStatus.CREATED)
    public SedeRespuestaDto crearSede(@Valid @RequestBody CrearSedeDto dto) { return servicio.crearSede(dto); }
    @GetMapping("/sedes")
    public List<SedeRespuestaDto> sedes() { return servicio.listarSedes(); }

    @PostMapping("/sedes/{sedeId}/aulas") @ResponseStatus(HttpStatus.CREATED)
    public AulaRespuestaDto crearAula(@PathVariable Long sedeId, @Valid @RequestBody CrearAulaDto dto) {
        return servicio.crearAula(sedeId, dto);
    }
    @GetMapping("/sedes/{sedeId}/aulas")
    public List<AulaRespuestaDto> aulas(@PathVariable Long sedeId) { return servicio.listarAulas(sedeId); }

    @PostMapping("/asignaciones") @ResponseStatus(HttpStatus.CREATED)
    public AsignacionRespuestaDto asignar(@Valid @RequestBody CrearAsignacionDto dto) { return servicio.asignarAula(dto); }
    @GetMapping("/aulas/{aulaId}/agenda")
    public List<AsignacionRespuestaDto> agenda(@PathVariable Long aulaId, @RequestParam LocalDate fecha) {
        return servicio.agendaAula(aulaId, fecha);
    }

    @PostMapping("/periodos") @ResponseStatus(HttpStatus.CREATED)
    public PeriodoRespuestaDto crearPeriodo(@Valid @RequestBody CrearPeriodoDto dto) { return servicio.crearPeriodo(dto); }
    @GetMapping("/periodos")
    public List<PeriodoRespuestaDto> periodos() { return servicio.listarPeriodos(); }

    @PostMapping("/turnos-examen") @ResponseStatus(HttpStatus.CREATED)
    public TurnoRespuestaDto crearTurno(@Valid @RequestBody CrearTurnoDto dto) { return servicio.crearTurno(dto); }
    @GetMapping("/turnos-examen")
    public List<TurnoRespuestaDto> turnos() { return servicio.listarTurnos(); }
}

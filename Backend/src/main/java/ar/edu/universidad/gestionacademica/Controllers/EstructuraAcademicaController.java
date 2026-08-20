package ar.edu.universidad.gestionacademica.Controllers;

import ar.edu.universidad.gestionacademica.BusinessLogic.EstructuraAcademicaServicio;
import ar.edu.universidad.gestionacademica.BusinessLogic.PlanEstudioPdfServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.*;

@RestController
@RequestMapping("/api/academica")
@RequiredArgsConstructor
public class EstructuraAcademicaController {
    private final EstructuraAcademicaServicio servicio;
    private final PlanEstudioPdfServicio pdfServicio;

    @PostMapping("/carreras")
    public ResponseEntity<CarreraRespuestaDto> crearCarrera(@Valid @RequestBody CrearCarreraDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearCarrera(dto));
    }
    @GetMapping("/carreras")
    public List<CarreraRespuestaDto> carreras() { return servicio.listarCarreras(); }
    @PutMapping("/carreras/{id}")
    public CarreraRespuestaDto actualizarCarrera(@PathVariable Long id, @Valid @RequestBody CrearCarreraDto dto) {
        return servicio.actualizarCarrera(id, dto);
    }
    @DeleteMapping("/carreras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarCarrera(@PathVariable Long id) { servicio.eliminarCarrera(id); }

    @PostMapping("/carreras/{carreraId}/planes")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanRespuestaDto crearPlan(@PathVariable Long carreraId, @Valid @RequestBody CrearPlanDto dto) {
        return servicio.crearPlan(carreraId, dto);
    }
    @GetMapping("/carreras/{carreraId}/planes")
    public List<PlanRespuestaDto> planes(@PathVariable Long carreraId) { return servicio.listarPlanes(carreraId); }

    @GetMapping("/planes/{planId}/pdf")
    public ResponseEntity<byte[]> descargarPlanPdf(@PathVariable Long planId) {
        byte[] pdf = pdfServicio.generar(planId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plan-estudios-" + planId + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/planes/{planId}/asignaturas")
    @ResponseStatus(HttpStatus.CREATED)
    public AsignaturaRespuestaDto crearAsignatura(@PathVariable Long planId, @Valid @RequestBody CrearAsignaturaDto dto) {
        return servicio.crearAsignatura(planId, dto);
    }
    @GetMapping("/planes/{planId}/asignaturas")
    public List<AsignaturaRespuestaDto> asignaturas(@PathVariable Long planId) { return servicio.listarAsignaturas(planId); }

    @PostMapping("/asignaturas/{asignaturaId}/correlatividades")
    @ResponseStatus(HttpStatus.CREATED)
    public CorrelatividadRespuestaDto agregarCorrelativa(@PathVariable Long asignaturaId,
                                                          @Valid @RequestBody CrearCorrelatividadDto dto) {
        return servicio.agregarCorrelatividad(asignaturaId, dto);
    }
    @GetMapping("/asignaturas/{asignaturaId}/correlatividades")
    public List<CorrelatividadRespuestaDto> correlatividades(@PathVariable Long asignaturaId) {
        return servicio.listarCorrelatividades(asignaturaId);
    }
}

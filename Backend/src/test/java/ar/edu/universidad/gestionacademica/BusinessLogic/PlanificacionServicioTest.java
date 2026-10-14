package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Aula;
import ar.edu.universidad.gestionacademica.Entidades.Sede;
import ar.edu.universidad.gestionacademica.Modelos.PlanificacionDto;
import ar.edu.universidad.gestionacademica.Repositorios.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlanificacionServicioTest {
    private final SedeRepositorio sedes = mock(SedeRepositorio.class);
    private final AulaRepositorio aulas = mock(AulaRepositorio.class);
    private final PlanificacionServicio servicio = new PlanificacionServicio(sedes, aulas,
            mock(AsignaturaRepositorio.class), mock(AsignacionAulaRepositorio.class),
            mock(PeriodoAcademicoRepositorio.class), mock(TurnoExamenRepositorio.class));

    @Test
    void guardaYDevuelveElNombreDelAula() {
        when(sedes.save(any(Sede.class))).thenAnswer(invocacion -> {
            Sede sede = invocacion.getArgument(0);
            sede.setId(2L);
            return sede;
        });

        var sede = servicio.crearSede(new PlanificacionDto.CrearSedeDto("Sede Centro", "Av. 123"));

        assertThat(sede.nombre()).isEqualTo("Sede Centro");
        when(sedes.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).build()));
        when(aulas.save(any(Aula.class))).thenAnswer(invocacion -> {
            Aula aula = invocacion.getArgument(0);
            aula.setId(4L);
            return aula;
        });

        var aula = servicio.crearAula(2L, new PlanificacionDto.CrearAulaDto(
                "A-4", 45, " Laboratorio 4 "));

        assertThat(aula.nombre()).isEqualTo("Laboratorio 4");
    }

    @Test
    void conservaCodigoComoNombreCuandoAulaNoTraeNombre() {
        when(sedes.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).build()));
        when(aulas.save(any(Aula.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var aula = servicio.crearAula(2L, new PlanificacionDto.CrearAulaDto("A-4", 45, null));

        assertThat(aula.nombre()).isEqualTo("A-4");
    }
}

package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Carrera;
import ar.edu.universidad.gestionacademica.Repositorios.AsignaturaRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CarreraRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.CorrelatividadRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.PlanEstudioRepositorio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.universidad.gestionacademica.Modelos.EstructuraAcademicaDto.CrearCarreraDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstructuraAcademicaServicioTest {
    private final CarreraRepositorio carreras = mock(CarreraRepositorio.class);
    private final AsignaturaRepositorio asignaturas = mock(AsignaturaRepositorio.class);
    private final EstructuraAcademicaServicio servicio = new EstructuraAcademicaServicio(
            carreras, mock(PlanEstudioRepositorio.class), asignaturas, mock(CorrelatividadRepositorio.class));

    @Test
    void creaLaCarreraSinDuracionInicial() {
        when(carreras.save(any(Carrera.class))).thenAnswer(invocacion -> {
            Carrera carrera = invocacion.getArgument(0);
            carrera.setId(4L);
            return carrera;
        });
        when(asignaturas.countByPlan_Carrera_Id(4L)).thenReturn(0L);

        var creada = servicio.crearCarrera(new CrearCarreraDto(
                "INF-001", "Ingeniería Informática", "Ingeniería", "Ingeniero/a"));

        assertThat(creada.duracion()).isEqualTo("0 años");
    }

    @Test
    void calculaUnAnioPorCadaDiezMaterias() {
        Carrera carrera = carreraDePrueba();
        when(carreras.findAll()).thenReturn(List.of(carrera));
        when(asignaturas.countByPlan_Carrera_Id(4L)).thenReturn(60L);

        assertThat(servicio.listarCarreras()).singleElement()
                .extracting(respuesta -> respuesta.duracion()).isEqualTo("6 años");
    }

    @Test
    void redondeaLaDuracionAlCuatrimestreNecesario() {
        Carrera carrera = carreraDePrueba();
        when(carreras.findAll()).thenReturn(List.of(carrera));
        when(asignaturas.countByPlan_Carrera_Id(4L)).thenReturn(14L);

        assertThat(servicio.listarCarreras()).singleElement()
                .extracting(respuesta -> respuesta.duracion()).isEqualTo("1,5 años");
    }

    private Carrera carreraDePrueba() {
        return Carrera.builder().id(4L).codigo("INF-001").nombre("Ingeniería Informática")
                .facultad("Ingeniería").titulo("Ingeniero/a").build();
    }
}

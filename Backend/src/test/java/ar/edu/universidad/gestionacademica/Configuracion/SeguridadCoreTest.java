package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.BusinessLogic.CursoServicio;
import ar.edu.universidad.gestionacademica.BusinessLogic.EstructuraAcademicaServicio;
import ar.edu.universidad.gestionacademica.BusinessLogic.UsuarioServicio;
import ar.edu.universidad.gestionacademica.Controllers.CursoController;
import ar.edu.universidad.gestionacademica.Controllers.EstructuraAcademicaController;
import ar.edu.universidad.gestionacademica.Controllers.InscripcionAlumnoController;
import ar.edu.universidad.gestionacademica.Controllers.InternoController;
import ar.edu.universidad.gestionacademica.Repositorios.CursoDocenteRepositorio;
import ar.edu.universidad.gestionacademica.Seguridad.AccesoAcademico;
import ar.edu.universidad.gestionacademica.Seguridad.ConvertidorJwtCore;
import ar.edu.universidad.gestionacademica.Seguridad.CorrelationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Autorización con el JWT de CORE: roles, permiso academic:manage, propiedad del recurso y tokens de servicio. */
@WebMvcTest({CursoController.class, EstructuraAcademicaController.class, InscripcionAlumnoController.class,
        InternoController.class})
@Import({ConfiguracionSeguridad.class, ManejadorErrores.class, AccesoAcademico.class})
class SeguridadCoreTest {
    private static final String ALUMNO = "8b0c1f6e-0000-0000-0000-00000000a1a1";
    private static final String DOCENTE = "8b0c1f6e-0000-0000-0000-00000000d0d0";

    @Autowired
    private MockMvc mvc;
    @MockBean
    private CursoServicio cursos;
    @MockBean
    private EstructuraAcademicaServicio estructura;
    @MockBean
    private UsuarioServicio usuarios;
    @MockBean
    private CursoDocenteRepositorio docentes;

    @Test
    void elAdministradorAcademicoDeCorePuedeEscribir() throws Exception {
        mvc.perform(put("/api/v1/planificacion/cursos/5/estado").with(usuario("admin-1", Map.of("roles", List.of("ACADEMIC_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/planificacion/cursos/5/estado")
                        .with(usuario("admin-2", Map.of("roles", List.of("STAFF"), "permissions", List.of("academic:manage"))))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unAlumnoLeeCatalogosPeroNoLosModifica() throws Exception {
        mvc.perform(get("/api/v1/academica/carreras").with(alumno())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/planificacion/cursos").with(alumno())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/academica/carreras").with(alumno())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ARQ-001\",\"nombre\":\"x\",\"facultad\":\"x\",\"titulo\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unAlumnoSoloVeSusPropiasInscripciones() throws Exception {
        mvc.perform(get("/api/v1/planificacion/alumnos/" + ALUMNO + "/inscripciones").with(alumno()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/planificacion/alumnos/otro-alumno/inscripciones").with(alumno()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAlumnoSoloPuedeInscribirseASiMismo() throws Exception {
        mvc.perform(post("/api/v1/planificacion/cursos/5/inscripciones").with(alumno())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alumnoId\":\"" + ALUMNO + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/planificacion/cursos/5/inscripciones").with(alumno())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"alumnoId\":\"otro-alumno\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(cursos, never()).inscribirAlumno(5L, new ar.edu.universidad.gestionacademica.Modelos.CursoDto.CrearInscripcionCursoDto("otro-alumno"));
    }

    @Test
    void unAlumnoNoVeLosDatosDeUnCursoPeroSiSuPropiaNota() throws Exception {
        mvc.perform(get("/api/v1/planificacion/cursos/5/alumnos").with(alumno())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/planificacion/cursos/5/notas/" + ALUMNO).with(alumno())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/planificacion/cursos/5/notas/otro-alumno").with(alumno())).andExpect(status().isForbidden());
    }

    @Test
    void unDocenteSoloOperaSobreLosCursosQueDicta() throws Exception {
        when(docentes.existsByCursoIdAndDocenteId(5L, DOCENTE)).thenReturn(true);

        mvc.perform(get("/api/v1/planificacion/cursos/5/alumnos").with(docente())).andExpect(status().isOk());
        mvc.perform(put("/api/v1/planificacion/cursos/5/notas/" + ALUMNO).with(docente())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"notaFinal\":8,\"estado\":\"APROBADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/planificacion/cursos/6/alumnos").with(docente())).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/planificacion/cursos/5/estado").with(docente())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void lasRutasInternasSoloAceptanTokensDeServicio() throws Exception {
        when(estructura.listarCarreras()).thenReturn(List.of());

        mvc.perform(get("/internal/v1/carreras").with(usuario("svc", Map.of("service", "backoffice-service"))))
                .andExpect(status().isOk());
        mvc.perform(get("/internal/v1/carreras/3/planes").with(usuario("svc", Map.of("service", "backoffice-service"))))
                .andExpect(status().isOk());
        mvc.perform(get("/internal/v1/carreras").with(alumno())).andExpect(status().isForbidden());
        mvc.perform(get("/internal/v1/carreras")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void laSesionLocalNoAlcanzaParaLasRutasInternas() throws Exception {
        mvc.perform(get("/internal/v1/carreras")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void laSesionLocalDelFrontendSigueFuncionandoEnApiV1() throws Exception {
        mvc.perform(get("/api/v1/planificacion/cursos/5/alumnos")).andExpect(status().isOk());
    }

    @Test
    void propagaElCorrelationIdRecibidoEnLosErrores() throws Exception {
        mvc.perform(get("/api/v1/planificacion/cursos").header(CorrelationId.HEADER, "corr-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("corr-123"));
    }

    private static JwtRequestPostProcessor alumno() {
        return usuario(ALUMNO, Map.of("roles", List.of("STUDENT")));
    }

    private static JwtRequestPostProcessor docente() {
        return usuario(DOCENTE, Map.of("roles", List.of("TEACHER")));
    }

    private static JwtRequestPostProcessor usuario(String sub, Map<String, Object> claims) {
        return jwt().jwt(token -> {
            token.subject(sub);
            claims.forEach(token::claim);
        }).authorities(token -> new ConvertidorJwtCore().convert(token).getAuthorities());
    }
}

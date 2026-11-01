package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.BusinessLogic.CursoServicio;
import ar.edu.universidad.gestionacademica.BusinessLogic.UsuarioServicio;
import ar.edu.universidad.gestionacademica.Controllers.AutenticacionController;
import ar.edu.universidad.gestionacademica.Controllers.CursoController;
import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import ar.edu.universidad.gestionacademica.Modelos.UsuarioDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AutenticacionController.class, CursoController.class})
@Import({ConfiguracionSeguridad.class, ManejadorErrores.class})
class SeguridadSesionTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private UsuarioServicio usuarios;
    @MockBean
    private CursoServicio cursos;

    @Test
    void sinSesionRespondeUnauthorizedConProblemJson() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = "alumno@universidad.edu.ar", roles = "USUARIO")
    void unUsuarioSinPermisoPuedeConsultarSuSesionYCerrarla() throws Exception {
        when(usuarios.buscarPorEmail("alumno@universidad.edu.ar")).thenReturn(new UsuarioDto(
                2L, "Alumno", "alumno@universidad.edu.ar", Usuario.Permiso.USUARIO, true));

        mvc.perform(get("/api/auth/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.permiso").value("USUARIO"));
        mvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alumno@universidad.edu.ar", roles = "USUARIO")
    void unUsuarioSinPermisoRecibeForbiddenConProblemJson() throws Exception {
        mvc.perform(get("/api/planificacion/cursos"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATIVO")
    void losErroresDeFormatoRespondenBadRequestYNoServerError() throws Exception {
        mvc.perform(get("/api/planificacion/cursos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(post("/api/planificacion/cursos").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}

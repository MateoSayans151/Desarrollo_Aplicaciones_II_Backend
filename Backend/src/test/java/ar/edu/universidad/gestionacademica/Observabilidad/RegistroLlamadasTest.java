package ar.edu.universidad.gestionacademica.Observabilidad;

import ar.edu.universidad.gestionacademica.BusinessLogic.EstructuraAcademicaServicio;
import ar.edu.universidad.gestionacademica.BusinessLogic.UsuarioServicio;
import ar.edu.universidad.gestionacademica.Repositorios.CursoDocenteRepositorio;
import ar.edu.universidad.gestionacademica.Configuracion.ConfiguracionSeguridad;
import ar.edu.universidad.gestionacademica.Configuracion.ManejadorErrores;
import ar.edu.universidad.gestionacademica.Controllers.EstructuraAcademicaController;
import ar.edu.universidad.gestionacademica.Excepciones.ReglaNegocioException;
import ar.edu.universidad.gestionacademica.Seguridad.AccesoAcademico;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstructuraAcademicaController.class)
@Import({ConfiguracionSeguridad.class, ManejadorErrores.class, AccesoAcademico.class, RegistroLlamadas.class})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "logging.formato=console")
class RegistroLlamadasTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private EstructuraAcademicaServicio servicio;
    @MockBean
    private JwtDecoder decodificador;
    @MockBean
    private UsuarioServicio usuarios;
    @MockBean
    private CursoDocenteRepositorio docentes;

    @Test
    void registraEntradaSalidaYConsumidor(CapturedOutput salida) throws Exception {
        when(servicio.listarPlanes(7L)).thenReturn(List.of());
        when(decodificador.decode(any())).thenReturn(Jwt.withTokenValue("t").header("alg", "RS256")
                .subject("admin-1").claim("roles", List.of("ACADEMIC_ADMIN")).build());
        mvc.perform(get("/api/v1/academica/carreras/7/planes")
                        .header("Authorization", "Bearer " + token(Map.of("sub", "admin-1", "roles", List.of("ACADEMIC_ADMIN"))))
                        .header("X-User-Id", "admin-1"))
                .andExpect(status().isOk());

        assertThat(salida.getOut())
                .contains("ENTRADA funcion=EstructuraAcademicaController.planes usuario=admin-1 params={\"carreraId\":7}")
                .contains("SALIDA funcion=EstructuraAcademicaController.planes")
                .contains("resultado=(0 elementos) []")
                .contains("HTTP GET /api/v1/academica/carreras/7/planes -> status=200")
                .contains("consumidor=core-gateway[ACADEMIC_ADMIN]");
    }

    @Test
    void registraLosErroresDeNegocio(CapturedOutput salida) throws Exception {
        when(servicio.crearCarrera(any())).thenThrow(new ReglaNegocioException("Ya existe una carrera con ese código"));
        when(decodificador.decode(any())).thenReturn(Jwt.withTokenValue("t").header("alg", "RS256")
                .subject("admin-1").claim("roles", List.of("ACADEMIC_ADMIN")).build());
        mvc.perform(post("/api/v1/academica/carreras")
                        .header("Authorization", "Bearer " + token(Map.of("sub", "admin-1", "roles", List.of("ACADEMIC_ADMIN"))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"ARQ-001\",\"nombre\":\"x\",\"facultad\":\"x\",\"titulo\":\"x\"}"))
                .andExpect(status().isBadRequest());

        assertThat(salida.getOut())
                .contains("ERROR funcion=EstructuraAcademicaController.crearCarrera")
                .contains("error=ReglaNegocioException mensaje=\"Ya existe una carrera con ese código\"")
                .contains("-> status=400");
    }

    @Test
    void enmascaraDatosSensibles() {
        RegistroLlamadas registro = new RegistroLlamadas(new ObjectMapper(), true, 2000);
        String json = registro.aJson(Map.of("dto", Map.of("email", "a@b.c", "password", "secreta")));
        assertThat(json).contains("\"password\":\"***\"").doesNotContain("secreta");
    }

    @Test
    void reconoceAlServicioDeBackoffice() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/v1/carreras");
        request.addHeader("Authorization", "Bearer " + token(Map.of("sub", "service:backoffice", "service", "backoffice")));
        assertThat(Consumidor.de(request).nombre()).isEqualTo("servicio:backoffice");
    }

    private static String token(Map<String, Object> claims) {
        try {
            Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
            return b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                    + b64.encodeToString(new ObjectMapper().writeValueAsBytes(claims)) + ".firma";
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

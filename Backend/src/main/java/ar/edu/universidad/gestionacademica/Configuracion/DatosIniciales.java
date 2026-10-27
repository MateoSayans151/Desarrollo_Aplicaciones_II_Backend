package ar.edu.universidad.gestionacademica.Configuracion;

import ar.edu.universidad.gestionacademica.Entidades.RegularidadCurso;
import ar.edu.universidad.gestionacademica.Entidades.Usuario;
import ar.edu.universidad.gestionacademica.Repositorios.CursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.RegularidadCursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DatosIniciales {
    @Bean
    CommandLineRunner crearAdministradorInicial(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder,
                                                @Value("${universidad.admin.email}") String email,
                                                @Value("${universidad.admin.password}") String password) {
        return args -> {
            if (!usuarios.existsByEmailIgnoreCase(email)) {
                usuarios.save(Usuario.builder()
                        .nombre("Administrador")
                        .email(email.toLowerCase())
                        .password(passwordEncoder.encode(password))
                        .permiso(Usuario.Permiso.ADMINISTRATIVO)
                        .activo(true)
                        .build());
            }
        };
    }

    @Bean
    CommandLineRunner crearRegularidadesFaltantes(CursoRepositorio cursos,
                                                   RegularidadCursoRepositorio regularidades) {
        return args -> cursos.findAll().stream()
                .filter(curso -> !regularidades.existsByCursoId(curso.getId()))
                .map(curso -> RegularidadCurso.builder()
                        .curso(curso)
                        .codigo(curso.getCodigo())
                        .asistenciaMinima(new BigDecimal("75.00"))
                        .promedioMinimo(new BigDecimal("6.00"))
                        .build())
                .forEach(regularidades::save);
    }
}

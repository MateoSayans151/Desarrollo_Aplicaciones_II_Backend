package ar.edu.universidad.gestionacademica.BusinessLogic;

import ar.edu.universidad.gestionacademica.Entidades.Curso;
import ar.edu.universidad.gestionacademica.Entidades.RegularidadCurso;
import ar.edu.universidad.gestionacademica.Excepciones.RecursoNoEncontradoException;
import ar.edu.universidad.gestionacademica.Repositorios.CursoRepositorio;
import ar.edu.universidad.gestionacademica.Repositorios.RegularidadCursoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.*;

@Service
@RequiredArgsConstructor
@Transactional
public class RegularidadServicio {
    private final CursoRepositorio cursos;
    private final RegularidadCursoRepositorio regularidades;

    public CondicionesRegularidadCursoDto consultar(Long cursoId) {
        Curso curso = cursos.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));
        RegularidadCurso regularidad = regularidades.findByCursoId(cursoId)
                .orElseGet(() -> regularidades.save(RegularidadCurso.builder()
                        .curso(curso)
                        .codigo(curso.getCodigo())
                        .asistenciaMinima(new BigDecimal("75.00"))
                        .promedioMinimo(new BigDecimal("6.00"))
                        .build()));
        return respuesta(regularidad);
    }

    public CondicionesRegularidadCursoDto actualizar(Long cursoId, GuardarCondicionesRegularidadDto datos) {
        Curso curso = cursos.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));
        RegularidadCurso regularidad = regularidades.findByCursoId(cursoId)
                .orElseGet(() -> RegularidadCurso.builder().curso(curso).codigo(curso.getCodigo()).build());
        regularidad.setCodigo(curso.getCodigo());
        regularidad.setAsistenciaMinima(datos.asistenciaMinima());
        regularidad.setPromedioMinimo(datos.promedioMinimo());
        return respuesta(regularidades.save(regularidad));
    }

    private CondicionesRegularidadCursoDto respuesta(RegularidadCurso regularidad) {
        return new CondicionesRegularidadCursoDto(regularidad.getId(), regularidad.getCurso().getId(),
                regularidad.getCodigo(), regularidad.getAsistenciaMinima(), regularidad.getPromedioMinimo());
    }
}

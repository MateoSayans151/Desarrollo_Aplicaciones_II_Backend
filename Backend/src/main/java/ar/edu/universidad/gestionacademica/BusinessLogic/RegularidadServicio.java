package ar.edu.universidad.gestionacademica.BusinessLogic;

import org.springframework.stereotype.Service;

import java.util.ArrayList;

import static ar.edu.universidad.gestionacademica.Modelos.RegularidadDto.*;

@Service
public class RegularidadServicio {
    public ResultadoRegularidadDto validar(ValidarRegularidadDto datos) {
        var motivos = new ArrayList<String>();
        if (datos.asistencia() < datos.asistenciaMinima()) motivos.add("No alcanza la asistencia minima");
        if (datos.promedio() < datos.promedioMinimo()) motivos.add("No alcanza el promedio minimo");
        boolean regular = motivos.isEmpty();
        return new ResultadoRegularidadDto(regular, regular, motivos);
    }
}

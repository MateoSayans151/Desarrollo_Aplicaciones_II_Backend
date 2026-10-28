package ar.edu.universidad.gestionacademica.Entidades;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EstadoPlanEstudioConverter implements AttributeConverter<EstadoPlanEstudio, String> {
    @Override
    public String convertToDatabaseColumn(EstadoPlanEstudio estado) {
        return estado == null ? null : estado.name();
    }

    @Override
    public EstadoPlanEstudio convertToEntityAttribute(String valor) {
        if (valor == null) return null;
        return switch (valor) {
            case "ACTIVO", "ACTIVA" -> EstadoPlanEstudio.ACTIVO;
            case "PLANIFICADO", "BORRADOR" -> EstadoPlanEstudio.PLANIFICADO;
            default -> throw new IllegalArgumentException("Estado de plan desconocido: " + valor);
        };
    }
}

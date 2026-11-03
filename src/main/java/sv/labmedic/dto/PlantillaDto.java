package sv.labmedic.dto;

import sv.labmedic.entity.Plantilla;

import java.util.List;

public record PlantillaDto(Long id, String codigo, String nombre, String categoria,
                           Integer version, boolean activa, List<String> campos) {

    public static PlantillaDto de(Plantilla p) {
        return new PlantillaDto(p.id, p.tipoExamen.codigo, p.tipoExamen.nombre,
                p.tipoExamen.categoria, p.version, p.activa, p.campos());
    }
}

package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.entity.Eglise;
import org.locationtech.jts.geom.Point;

public final class EgliseMapper {

    private EgliseMapper() {
    }

    public static EgliseDTO toDTO(Eglise eglise) {
        EgliseDTO dto = new EgliseDTO();
        dto.setId(eglise.getId());
        dto.setNom(eglise.getNom());
        dto.setAdresse(eglise.getAdresse());
        dto.setHistorique(eglise.getHistorique());
        dto.setLienFacebook(eglise.getLienFacebook());
        dto.setParoisseId(eglise.getParoisse() != null ? eglise.getParoisse().getId() : null);
        dto.setResponsableId(eglise.getResponsable() != null ? eglise.getResponsable().getId() : null);

        Point point = eglise.getLocalisation();
        if (point != null) {
            // En JTS : getX() = longitude, getY() = latitude (ordre inversé par rapport à l'intuition)
            dto.setLongitude(point.getX());
            dto.setLatitude(point.getY());
        } else {
            dto.setLongitude(null);
            dto.setLatitude(null);
        }

        return dto;
    }
}

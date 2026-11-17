package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.RessourceDTO;
import com.anglikana.backoffice.entity.Ressource;

public final class RessourceMapper {

    private RessourceMapper() {
    }

    public static RessourceDTO toDTO(Ressource ressource) {
        RessourceDTO dto = new RessourceDTO();
        dto.setId(ressource.getId());
        dto.setNom(ressource.getNom());
        dto.setAdresse(ressource.getAdresse());
        dto.setTypeId(ressource.getType() != null ? ressource.getType().getId() : null);
        dto.setEgliseId(ressource.getEglise() != null ? ressource.getEglise().getId() : null);
        dto.setResponsableId(ressource.getResponsable() != null ? ressource.getResponsable().getId() : null);
        return dto;
    }
}

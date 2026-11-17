package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.TypeRessourceDTO;
import com.anglikana.backoffice.entity.TypeRessource;

public final class TypeRessourceMapper {

    private TypeRessourceMapper() {
    }

    public static TypeRessourceDTO toDTO(TypeRessource typeRessource) {
        TypeRessourceDTO dto = new TypeRessourceDTO();
        dto.setId(typeRessource.getId());
        dto.setNom(typeRessource.getNom());
        return dto;
    }
}

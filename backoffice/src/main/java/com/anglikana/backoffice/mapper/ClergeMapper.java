package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.ClergeDTO;
import com.anglikana.backoffice.entity.Clerge;

public final class ClergeMapper {

    private ClergeMapper() {
    }

    public static ClergeDTO toDTO(Clerge clerge) {
        ClergeDTO dto = new ClergeDTO();
        dto.setId(clerge.getId());
        dto.setNom(clerge.getNom());
        dto.setPrenom(clerge.getPrenom());
        dto.setDateNaissance(clerge.getDateNaissance());
        dto.setHierarchie(clerge.getHierarchie());
        dto.setTelephone(clerge.getTelephone());
        dto.setEmail(clerge.getEmail());
        dto.setRoleId(clerge.getRole() != null ? clerge.getRole().getId() : null);
        return dto;
    }
}

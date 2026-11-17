package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.entity.Paroisse;

public final class ParoisseMapper {

    private ParoisseMapper() {
    }

    public static ParoisseDTO toDTO(Paroisse paroisse) {
        ParoisseDTO dto = new ParoisseDTO();
        dto.setId(paroisse.getId());
        dto.setNom(paroisse.getNom());
        dto.setCarteQgis(GeometrieMapper.versGeoJson(paroisse.getCarteQgis()));
        dto.setDistrictId(paroisse.getDistrict() != null ? paroisse.getDistrict().getId() : null);
        dto.setResponsableId(paroisse.getResponsable() != null ? paroisse.getResponsable().getId() : null);
        return dto;
    }
}

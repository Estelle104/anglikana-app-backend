package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.entity.District;

public final class DistrictMapper {

    private DistrictMapper() {
    }

    public static DistrictDTO toDTO(District district) {
        DistrictDTO dto = new DistrictDTO();
        dto.setId(district.getId());
        dto.setNom(district.getNom());
        dto.setCarteQgis(GeometrieMapper.versGeoJson(district.getCarteQgis()));
        dto.setRegionId(district.getRegion() != null ? district.getRegion().getId() : null);
        dto.setResponsableId(district.getResponsable() != null ? district.getResponsable().getId() : null);
        return dto;
    }
}

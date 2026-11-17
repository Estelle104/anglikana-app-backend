package com.anglikana.backoffice.mapper;

import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.entity.Region;

public final class RegionMapper {

    private RegionMapper() {
    }

    public static RegionDTO toDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getId());
        dto.setNom(region.getNom());
        dto.setCarteQgis(GeometrieMapper.versGeoJson(region.getCarteQgis()));
        dto.setDioceseId(region.getDiocese() != null ? region.getDiocese().getId() : null);
        dto.setResponsableId(region.getResponsable() != null ? region.getResponsable().getId() : null);
        return dto;
    }
}

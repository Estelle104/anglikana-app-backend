package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.RegionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/districts")
public class DistrictController {

    private final DistrictRepository districtRepository;
    private final RegionRepository regionRepository;
    private final ClergeRepository clergeRepository;

    public DistrictController(DistrictRepository districtRepository,
                               RegionRepository regionRepository,
                               ClergeRepository clergeRepository) {
        this.districtRepository = districtRepository;
        this.regionRepository = regionRepository;
        this.clergeRepository = clergeRepository;
    }

    @GetMapping
    public List<DistrictDTO> getAll() {
        return districtRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictDTO> getById(@PathVariable Integer id) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            return ResponseEntity.ok(toDTO(district));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DistrictDTO> create(@RequestBody DistrictDTO dto) {
        District district = new District();
        appliquerDTO(district, dto);

        District saved = districtRepository.save(district);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DistrictDTO> update(@PathVariable Integer id, @RequestBody DistrictDTO dto) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            appliquerDTO(district, dto);
            District updated = districtRepository.save(district);
            return ResponseEntity.ok(toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!districtRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        districtRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private DistrictDTO toDTO(District district) {
        DistrictDTO dto = new DistrictDTO();
        dto.setId(district.getId());
        dto.setNom(district.getNom());
        dto.setCarteQgis(district.getCarteQgis());
        dto.setRegionId(district.getRegion() != null ? district.getRegion().getId() : null);
        dto.setResponsableId(district.getResponsable() != null ? district.getResponsable().getId() : null);
        return dto;
    }

    private void appliquerDTO(District district, DistrictDTO dto) {
        district.setNom(dto.getNom());
        district.setCarteQgis(dto.getCarteQgis());

        if (dto.getRegionId() != null) {
            Optional<Region> regionOptionnel = regionRepository.findById(dto.getRegionId());
            if (regionOptionnel.isPresent()) {
                district.setRegion(regionOptionnel.get());
            } else {
                district.setRegion(null);
            }
        } else {
            district.setRegion(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                district.setResponsable(clergeOptionnel.get());
            } else {
                district.setResponsable(null);
            }
        } else {
            district.setResponsable(null);
        }
    }
}
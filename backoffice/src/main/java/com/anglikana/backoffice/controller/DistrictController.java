package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.mapper.DistrictMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.RegionRepository;
import com.anglikana.backoffice.service.HierarchieService;

import org.locationtech.jts.geom.Polygon;
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
    private final HierarchieService hierarchieService;

    public DistrictController(DistrictRepository districtRepository,
                               RegionRepository regionRepository,
                               ClergeRepository clergeRepository,
                               HierarchieService hierarchieService) {
        this.districtRepository = districtRepository;
        this.regionRepository = regionRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public List<DistrictDTO> getAll() {
        return districtRepository.findAll()
                .stream().map(DistrictMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistrictDTO> getById(@PathVariable Integer id) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            return ResponseEntity.ok(DistrictMapper.toDTO(district));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/paroisses")
    public ResponseEntity<List<ParoisseDTO>> getParoisses(@PathVariable Integer id) {
        return hierarchieService.getParoissesDuDistrict(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody DistrictDTO dto) {
        District district = new District();
        try {
            appliquerDTO(district, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        District saved = districtRepository.save(district);
        return ResponseEntity.status(201).body(DistrictMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody DistrictDTO dto) {
        Optional<District> districtOptionnel = districtRepository.findById(id);

        if (districtOptionnel.isPresent()) {
            District district = districtOptionnel.get();
            try {
                appliquerDTO(district, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            District updated = districtRepository.save(district);
            return ResponseEntity.ok(DistrictMapper.toDTO(updated));
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

    private void appliquerDTO(District district, DistrictDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        district.setCarteQgis(carteQgis);
        district.setNom(dto.getNom());

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
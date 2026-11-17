package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.dto.DistrictDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.mapper.RegionMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
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
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionRepository regionRepository;
    private final DioceseRepository dioceseRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;

    public RegionController(RegionRepository regionRepository,DioceseRepository dioceseRepository,ClergeRepository clergeRepository,
                            HierarchieService hierarchieService) {
        this.regionRepository = regionRepository;
        this.dioceseRepository = dioceseRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public List<RegionDTO> getAll() {
        return regionRepository.findAll()
                .stream().map(RegionMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegionDTO> getById(@PathVariable Integer id) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            return ResponseEntity.ok(RegionMapper.toDTO(region));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/districts")
    public ResponseEntity<List<DistrictDTO>> getDistricts(@PathVariable Integer id) {
        return hierarchieService.getDistrictsDeLaRegion(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody RegionDTO dto) {
        Region region = new Region();
        try {
            appliquerDTO(region, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        Region saved = regionRepository.save(region);
        return ResponseEntity.status(201).body(RegionMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody RegionDTO dto) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            try {
                appliquerDTO(region, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            Region updated = regionRepository.save(region);
            return ResponseEntity.ok(RegionMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!regionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        regionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Region region, RegionDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        region.setCarteQgis(carteQgis);
        region.setNom(dto.getNom());

        if (dto.getDioceseId() != null) {
            Optional<Diocese> dioceseOptionnel = dioceseRepository.findById(dto.getDioceseId());
            if (dioceseOptionnel.isPresent()) {
                region.setDiocese(dioceseOptionnel.get());
            } else {
                region.setDiocese(null);
            }
        } else {
            region.setDiocese(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                region.setResponsable(clergeOptionnel.get());
            } else {
                region.setResponsable(null);
            }
        } else {
            region.setResponsable(null);
        }
    }
}
package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.RegionDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.entity.Region;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;
import com.anglikana.backoffice.repository.RegionRepository;

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

    public RegionController(RegionRepository regionRepository,DioceseRepository dioceseRepository,ClergeRepository clergeRepository) {
        this.regionRepository = regionRepository;
        this.dioceseRepository = dioceseRepository;
        this.clergeRepository = clergeRepository;
    }

    @GetMapping
    public List<RegionDTO> getAll() {
        return regionRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegionDTO> getById(@PathVariable Integer id) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            return ResponseEntity.ok(toDTO(region));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionDTO> create(@RequestBody RegionDTO dto) {
        Region region = new Region();
        appliquerDTO(region, dto);

        Region saved = regionRepository.save(region);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RegionDTO> update(@PathVariable Integer id, @RequestBody RegionDTO dto) {
        Optional<Region> regionOptionnel = regionRepository.findById(id);

        if (regionOptionnel.isPresent()) {
            Region region = regionOptionnel.get();
            appliquerDTO(region, dto);
            Region updated = regionRepository.save(region);
            return ResponseEntity.ok(toDTO(updated));
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

    private RegionDTO toDTO(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getId());
        dto.setNom(region.getNom());
        dto.setDioceseId(region.getDiocese() != null ? region.getDiocese().getId() : null);
        dto.setResponsableId(region.getResponsable() != null ? region.getResponsable().getId() : null);
        return dto;
    }

    private void appliquerDTO(Region region, RegionDTO dto) {
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
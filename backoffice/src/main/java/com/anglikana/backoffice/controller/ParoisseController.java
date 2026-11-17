package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.mapper.ParoisseMapper;
import com.anglikana.backoffice.mapper.GeometrieMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
import com.anglikana.backoffice.service.HierarchieService;

import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/paroisses")
public class ParoisseController {

    private final ParoisseRepository paroisseRepository;
    private final DistrictRepository districtRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;

    public ParoisseController(ParoisseRepository paroisseRepository,
                               DistrictRepository districtRepository,
                               ClergeRepository clergeRepository,
                               HierarchieService hierarchieService) {
        this.paroisseRepository = paroisseRepository;
        this.districtRepository = districtRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public List<ParoisseDTO> getAll() {
        return paroisseRepository.findAll()
                .stream().map(ParoisseMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParoisseDTO> getById(@PathVariable Integer id) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            return ResponseEntity.ok(ParoisseMapper.toDTO(paroisse));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/eglises")
    public ResponseEntity<List<EgliseDTO>> getEglises(@PathVariable Integer id) {
        return hierarchieService.getEglisesDeLaParoisse(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody ParoisseDTO dto) {
        Paroisse paroisse = new Paroisse();
        try {
            appliquerDTO(paroisse, dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        Paroisse saved = paroisseRepository.save(paroisse);
        return ResponseEntity.status(201).body(ParoisseMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody ParoisseDTO dto) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            try {
                appliquerDTO(paroisse, dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
            Paroisse updated = paroisseRepository.save(paroisse);
            return ResponseEntity.ok(ParoisseMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!paroisseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        paroisseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Paroisse paroisse, ParoisseDTO dto) {
        Polygon carteQgis = GeometrieMapper.depuisGeoJson(dto.getCarteQgis());
        paroisse.setCarteQgis(carteQgis);
        paroisse.setNom(dto.getNom());

        if (dto.getDistrictId() != null) {
            Optional<District> districtOptionnel = districtRepository.findById(dto.getDistrictId());
            if (districtOptionnel.isPresent()) {
                paroisse.setDistrict(districtOptionnel.get());
            } else {
                paroisse.setDistrict(null);
            }
        } else {
            paroisse.setDistrict(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                paroisse.setResponsable(clergeOptionnel.get());
            } else {
                paroisse.setResponsable(null);
            }
        } else {
            paroisse.setResponsable(null);
        }
    }
}
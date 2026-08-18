package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ParoisseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.District;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DistrictRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;

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

    public ParoisseController(ParoisseRepository paroisseRepository,
                               DistrictRepository districtRepository,
                               ClergeRepository clergeRepository) {
        this.paroisseRepository = paroisseRepository;
        this.districtRepository = districtRepository;
        this.clergeRepository = clergeRepository;
    }

    @GetMapping
    public List<ParoisseDTO> getAll() {
        return paroisseRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParoisseDTO> getById(@PathVariable Integer id) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            return ResponseEntity.ok(toDTO(paroisse));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParoisseDTO> create(@RequestBody ParoisseDTO dto) {
        Paroisse paroisse = new Paroisse();
        appliquerDTO(paroisse, dto);

        Paroisse saved = paroisseRepository.save(paroisse);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParoisseDTO> update(@PathVariable Integer id, @RequestBody ParoisseDTO dto) {
        Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(id);

        if (paroisseOptionnel.isPresent()) {
            Paroisse paroisse = paroisseOptionnel.get();
            appliquerDTO(paroisse, dto);
            Paroisse updated = paroisseRepository.save(paroisse);
            return ResponseEntity.ok(toDTO(updated));
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

    private ParoisseDTO toDTO(Paroisse paroisse) {
        ParoisseDTO dto = new ParoisseDTO();
        dto.setId(paroisse.getId());
        dto.setNom(paroisse.getNom());
        dto.setCarteQgis(paroisse.getCarteQgis());
        dto.setDistrictId(paroisse.getDistrict() != null ? paroisse.getDistrict().getId() : null);
        dto.setResponsableId(paroisse.getResponsable() != null ? paroisse.getResponsable().getId() : null);
        return dto;
    }

    private void appliquerDTO(Paroisse paroisse, ParoisseDTO dto) {
        paroisse.setNom(dto.getNom());
        paroisse.setCarteQgis(dto.getCarteQgis());

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
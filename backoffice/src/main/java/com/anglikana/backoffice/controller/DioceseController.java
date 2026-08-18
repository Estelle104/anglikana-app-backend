package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.DioceseDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Diocese;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.DioceseRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dioceses")
public class DioceseController {

    private final DioceseRepository dioceseRepository;
    private final ClergeRepository clergeRepository;

    public DioceseController(DioceseRepository dioceseRepository,
                              ClergeRepository clergeRepository) {
        this.dioceseRepository = dioceseRepository;
        this.clergeRepository = clergeRepository;
    }

    @GetMapping
    public List<DioceseDTO> getAll() {
        return dioceseRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DioceseDTO> getById(@PathVariable Integer id) {
        return dioceseRepository.findById(id)
                .map(diocese -> ResponseEntity.ok(toDTO(diocese)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DioceseDTO> create(@RequestBody DioceseDTO dto) {
        Diocese diocese = new Diocese();
        appliquerDTO(diocese, dto);

        Diocese saved = dioceseRepository.save(diocese);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DioceseDTO> update(@PathVariable Integer id,
                                              @RequestBody DioceseDTO dto) {
        return dioceseRepository.findById(id)
                .map(diocese -> {
                    appliquerDTO(diocese, dto);
                    Diocese updated = dioceseRepository.save(diocese);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!dioceseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dioceseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private DioceseDTO toDTO(Diocese diocese) {
        DioceseDTO dto = new DioceseDTO();
        dto.setId(diocese.getId());
        dto.setNom(diocese.getNom());
        dto.setCarteQgis(diocese.getCarteQgis());
        dto.setPresentation(diocese.getPresentation());
        dto.setCoordonnees(diocese.getCoordonnees());
        dto.setResponsableId(
                diocese.getResponsable() != null ? diocese.getResponsable().getId() : null
        );
        return dto;
    }

    private void appliquerDTO(Diocese diocese, DioceseDTO dto) {
        diocese.setNom(dto.getNom());
        diocese.setCarteQgis(dto.getCarteQgis());
        diocese.setPresentation(dto.getPresentation());
        diocese.setCoordonnees(dto.getCoordonnees());

        if (dto.getResponsableId() != null) {
            Clerge clerge = clergeRepository.findById(dto.getResponsableId())
                    .orElse(null);
            diocese.setResponsable(clerge);
        } else {
            diocese.setResponsable(null);
        }
    }
}
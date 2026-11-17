package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.EgliseDTO;
import com.anglikana.backoffice.dto.RessourceDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Eglise;
import com.anglikana.backoffice.entity.Paroisse;
import com.anglikana.backoffice.mapper.EgliseMapper;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.EgliseRepository;
import com.anglikana.backoffice.repository.ParoisseRepository;
import com.anglikana.backoffice.service.HierarchieService;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/eglises")
public class EgliseController {

    // SRID 4326 = système de coordonnées GPS standard (WGS 84), cohérent avec ton schéma SQL
    private static final int SRID_GPS = 4326;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID_GPS);

    private final EgliseRepository egliseRepository;
    private final ParoisseRepository paroisseRepository;
    private final ClergeRepository clergeRepository;
    private final HierarchieService hierarchieService;

    public EgliseController(EgliseRepository egliseRepository,
                             ParoisseRepository paroisseRepository,
                             ClergeRepository clergeRepository,
                             HierarchieService hierarchieService) {
        this.egliseRepository = egliseRepository;
        this.paroisseRepository = paroisseRepository;
        this.clergeRepository = clergeRepository;
        this.hierarchieService = hierarchieService;
    }

    @GetMapping
    public List<EgliseDTO> getAll() {
        return egliseRepository.findAll()
                .stream().map(EgliseMapper::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EgliseDTO> getById(@PathVariable Integer id) {
        Optional<Eglise> egliseOptionnel = egliseRepository.findById(id);

        if (egliseOptionnel.isPresent()) {
            Eglise eglise = egliseOptionnel.get();
            return ResponseEntity.ok(EgliseMapper.toDTO(eglise));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/ressources")
    public ResponseEntity<List<RessourceDTO>> getRessources(@PathVariable Integer id) {
        return hierarchieService.getRessourcesDeLEglise(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EgliseDTO> create(@RequestBody EgliseDTO dto) {
        Eglise eglise = new Eglise();
        appliquerDTO(eglise, dto);

        Eglise saved = egliseRepository.save(eglise);
        return ResponseEntity.status(201).body(EgliseMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EgliseDTO> update(@PathVariable Integer id, @RequestBody EgliseDTO dto) {
        Optional<Eglise> egliseOptionnel = egliseRepository.findById(id);

        if (egliseOptionnel.isPresent()) {
            Eglise eglise = egliseOptionnel.get();
            appliquerDTO(eglise, dto);
            Eglise updated = egliseRepository.save(eglise);
            return ResponseEntity.ok(EgliseMapper.toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!egliseRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        egliseRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void appliquerDTO(Eglise eglise, EgliseDTO dto) {
        eglise.setNom(dto.getNom());
        eglise.setAdresse(dto.getAdresse());
        eglise.setHistorique(dto.getHistorique());
        eglise.setLienFacebook(dto.getLienFacebook());

        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            Coordinate coordinate = new Coordinate(dto.getLongitude(), dto.getLatitude());
            Point point = geometryFactory.createPoint(coordinate);
            eglise.setLocalisation(point);
        } else {
            eglise.setLocalisation(null);
        }

        if (dto.getParoisseId() != null) {
            Optional<Paroisse> paroisseOptionnel = paroisseRepository.findById(dto.getParoisseId());
            if (paroisseOptionnel.isPresent()) {
                eglise.setParoisse(paroisseOptionnel.get());
            } else {
                eglise.setParoisse(null);
            }
        } else {
            eglise.setParoisse(null);
        }

        if (dto.getResponsableId() != null) {
            Optional<Clerge> clergeOptionnel = clergeRepository.findById(dto.getResponsableId());
            if (clergeOptionnel.isPresent()) {
                eglise.setResponsable(clergeOptionnel.get());
            } else {
                eglise.setResponsable(null);
            }
        } else {
            eglise.setResponsable(null);
        }
    }
}
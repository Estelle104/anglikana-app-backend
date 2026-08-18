package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.ClergeDTO;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Role;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.RoleRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clerges")
public class ClergeController {

    private final ClergeRepository clergeRepository;
    private final RoleRepository roleRepository;

    public ClergeController(ClergeRepository clergeRepository,
                             RoleRepository roleRepository) {
        this.clergeRepository = clergeRepository;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public List<ClergeDTO> getAll() {
        return clergeRepository.findAll()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClergeDTO> getById(@PathVariable Integer id) {
        Optional<Clerge> clergeOptionnel = clergeRepository.findById(id);

        if (clergeOptionnel.isPresent()) {
            Clerge clerge = clergeOptionnel.get();
            return ResponseEntity.ok(toDTO(clerge));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClergeDTO> create(@RequestBody ClergeDTO dto) {
        Clerge clerge = new Clerge();
        appliquerDTO(clerge, dto);

        Clerge saved = clergeRepository.save(clerge);
        return ResponseEntity.status(201).body(toDTO(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClergeDTO> update(@PathVariable Integer id, @RequestBody ClergeDTO dto) {
        Optional<Clerge> clergeOptionnel = clergeRepository.findById(id);

        if (clergeOptionnel.isPresent()) {
            Clerge clerge = clergeOptionnel.get();
            appliquerDTO(clerge, dto);
            Clerge updated = clergeRepository.save(clerge);
            return ResponseEntity.ok(toDTO(updated));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        if (!clergeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clergeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ClergeDTO toDTO(Clerge clerge) {
        ClergeDTO dto = new ClergeDTO();
        dto.setId(clerge.getId());
        dto.setNom(clerge.getNom());
        dto.setPrenom(clerge.getPrenom());
        dto.setDateNaissance(clerge.getDateNaissance());
        dto.setHierarchie(clerge.getHierarchie());
        dto.setTelephone(clerge.getTelephone());
        dto.setEmail(clerge.getEmail());
        dto.setRoleId(clerge.getRole() != null ? clerge.getRole().getId() : null);
        return dto;
    }

    private void appliquerDTO(Clerge clerge, ClergeDTO dto) {
        clerge.setNom(dto.getNom());
        clerge.setPrenom(dto.getPrenom());
        clerge.setDateNaissance(dto.getDateNaissance());
        clerge.setHierarchie(dto.getHierarchie());
        clerge.setTelephone(dto.getTelephone());
        clerge.setEmail(dto.getEmail());

        if (dto.getRoleId() != null) {
            Optional<Role> roleOptionnel = roleRepository.findById(dto.getRoleId());
            if (roleOptionnel.isPresent()) {
                clerge.setRole(roleOptionnel.get());
            } else {
                clerge.setRole(null);
            }
        } else {
            clerge.setRole(null);
        }
    }
}
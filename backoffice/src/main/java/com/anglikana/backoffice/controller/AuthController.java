package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.LoginRequest;
import com.anglikana.backoffice.dto.LoginResponse;
import com.anglikana.backoffice.dto.RegisterRequest;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Utilisateur;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.UtilisateurRepository;
import com.anglikana.backoffice.service.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final ClergeRepository clergeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UtilisateurRepository utilisateurRepository,
                           ClergeRepository clergeRepository,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
        this.clergeRepository = clergeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getNomUtilisateur(),
                            request.getMotDePasse()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String role = extraireRole(userDetails);
            String token = jwtService.generateToken(userDetails.getUsername(), role);

            return ResponseEntity.ok(new LoginResponse(token, userDetails.getUsername(), role));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (utilisateurRepository.existsByNomUtilisateur(request.getNomUtilisateur())) {
            return ResponseEntity.status(409).body("Ce nom d'utilisateur existe déjà");
        }

        Clerge clerge = clergeRepository.findById(request.getClergeId())
                .orElse(null);

        if (clerge == null) {
            return ResponseEntity.badRequest().body("Clergé introuvable pour l'id fourni");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNomUtilisateur(request.getNomUtilisateur());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setActif(true);
        utilisateur.setClerge(clerge);

        utilisateurRepository.save(utilisateur);

        return ResponseEntity.status(201).body("Compte créé avec succès pour : " + utilisateur.getNomUtilisateur());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Déconnexion réussie. Supprimez le token côté client.");
    }

    private String extraireRole(UserDetails userDetails) {
        Optional<? extends GrantedAuthority> authority =
                userDetails.getAuthorities().stream().findFirst();

        return authority.map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER")
                .replace("ROLE_", "");
    }
}
package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.LoginRequest;
import com.anglikana.backoffice.dto.LoginResponse;
import com.anglikana.backoffice.dto.RegisterRequest;
import com.anglikana.backoffice.entity.Clerge;
import com.anglikana.backoffice.entity.Utilisateur;
import com.anglikana.backoffice.repository.ClergeRepository;
import com.anglikana.backoffice.repository.UtilisateurRepository;
import com.anglikana.backoffice.service.JwtService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "http://localhost:8084", "http://127.0.0.1:8084"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
@Tag(name = "Authentification", description = "Connexion par JWT, création de compte et déconnexion (routes publiques)")
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
    @Operation(summary = "Se connecter",
            description = "Vérifie le nom d'utilisateur et le mot de passe, puis renvoie un jeton JWT (validité : jwt.expiration, 24 h par défaut). "
                    + "Le rôle renvoyé est celui du clergé lié au compte, sans préfixe ROLE_. "
                    + "Envoyer ensuite le jeton dans l'en-tête Authorization: Bearer <jeton> (bouton « Authorize »).")
    @ApiResponse(responseCode = "200", description = "Connexion réussie",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "401", description = "Nom d'utilisateur ou mot de passe incorrect",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
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
    @Operation(summary = "Créer un compte",
            description = "Crée un compte actif rattaché à un membre du clergé (clergeId) ; le mot de passe est enregistré en BCrypt. "
                    + "Le compte reçoit les droits du rôle de ce clergé. Route actuellement publique.")
    @ApiResponse(responseCode = "201", description = "Compte créé (message de confirmation)",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "400", description = "Clergé introuvable pour l'id fourni",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    @ApiResponse(responseCode = "409", description = "Ce nom d'utilisateur existe déjà",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
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
    @Operation(summary = "Se déconnecter",
            description = "Sans effet côté serveur (JWT sans état) : le client doit supprimer son jeton.")
    @ApiResponse(responseCode = "200", description = "Message de confirmation",
            content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
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
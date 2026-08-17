package com.anglikana.backoffice.controller;

import com.anglikana.backoffice.dto.LoginRequest;
import com.anglikana.backoffice.dto.LoginResponse;
import com.anglikana.backoffice.service.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
                           JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

            LoginResponse response = new LoginResponse(
                    token,
                    userDetails.getUsername(),
                    role
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    private String extraireRole(UserDetails userDetails) {
        Optional<? extends GrantedAuthority> authority =
                userDetails.getAuthorities().stream().findFirst();

        return authority.map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER")
                .replace("ROLE_", "");
    }
}
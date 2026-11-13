package com.anglikana.backoffice;

import com.anglikana.backoffice.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void expiredTokenShouldBeRejectedGracefully() {
        JwtService jwtService = new JwtService();
        String secret = "SzUjbVEhMnhGdjgqclo0JHdMMV9uTjdAYkM1JmpSMyV0RzYoeVg5KQ==";

        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", 1000L);

        String expiredToken = Jwts.builder()
                .subject("admin")
                .issuedAt(new Date(System.currentTimeMillis() - 5000L))
                .expiration(new Date(System.currentTimeMillis() - 1000L))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.extractUsername(expiredToken)).isNull();
        assertThat(jwtService.isTokenValid(expiredToken, "admin")).isFalse();
    }
}

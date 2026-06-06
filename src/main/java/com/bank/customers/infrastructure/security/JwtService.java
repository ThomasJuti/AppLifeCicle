package com.bank.customers.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(
        @Value("${app.security.jwt.secret}") String secret,
        @Value("${app.security.jwt.expiration-seconds:3600}") long expirationSeconds
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "app.security.jwt.secret debe tener al menos 32 caracteres (256 bits) para HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    public Optional<String> extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}

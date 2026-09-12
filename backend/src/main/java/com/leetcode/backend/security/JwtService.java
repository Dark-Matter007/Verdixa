package com.leetcode.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import com.leetcode.backend.model.User;

@Service
public class JwtService {

    private static final long EXPIRATION_TIME =
            1000L * 60 * 60 * 24; // 24 hours

    private final SecretKey key;

    public JwtService(@Value("${algosphere.jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("ALGOSPHERE_JWT_SECRET must be at least 32 characters.");
        }
        this.key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User user) {

        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + EXPIRATION_TIME
        );

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("uid", user.getId())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("ver", user.getAuthVersion())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) { return numberClaim(token, "uid").longValue(); }

    public long extractAuthVersion(String token) { return numberClaim(token, "ver").longValue(); }

    public String extractRole(String token) {

        return getClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValidFor(String token, User user) {
        try {
            return isTokenValid(token)
                    && user.getId().equals(extractUserId(token))
                    && user.getAuthVersion() == extractAuthVersion(token)
                    && user.getRole().name().equals(extractRole(token));
        } catch (Exception exception) {
            return false;
        }
    }

    private Number numberClaim(String token, String name) {
        Object value = getClaims(token).get(name);
        if (value instanceof Number number) return number;
        throw new IllegalArgumentException("Missing numeric JWT claim.");
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

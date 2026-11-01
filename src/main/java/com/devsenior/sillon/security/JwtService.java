package com.devsenior.sillon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    private SecretKey obtenerClaveFirma() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token JWT incorporando el salonId y el rol como claims adicionales.
     */
    public String generarToken(UsuarioPrincipal usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("salonId", usuario.getSalonId());
        claims.put("rol", usuario.getRol());

        Date ahora = new Date();
        Date fechaExpiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(usuario.getEmail())
                .issuedAt(ahora)
                .expiration(fechaExpiracion)
                .signWith(obtenerClaveFirma())
                .compact();
    }

    /**
     * Extrae el email (subject) contenido en el token.
     */
    public String obtenerEmailDelToken(String token) {
        return obtenerClaims(token).getSubject();
    }

    /**
     * Extrae el salonId contenido en los claims del token.
     */
    public Long obtenerSalonIdDelToken(String token) {
        Object salonId = obtenerClaims(token).get("salonId");
        if (salonId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Valida la firma y la vigencia del token.
     */
    public boolean esTokenValido(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(obtenerClaveFirma())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
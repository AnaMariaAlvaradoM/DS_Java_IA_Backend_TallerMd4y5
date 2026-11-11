package com.devsenior.sillon.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;
    private final long expiracionMinutos;

    public JwtService(
            @Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expiracion-minutos:1440}") long expiracionMinutos) {
        this.secretKey = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
        this.jwtExpirationMs = expiracionMinutos * 60 * 1000;
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }

    public String generarToken(UsuarioPrincipal usuario) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(usuario.getUsername()) // email
                .claim("id", usuario.getId())
                .claim("salonId", usuario.getSalonId())
                .claim("rol", usuario.getRol())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(secretKey)
                .compact();
    }

    public Claims obtenerClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
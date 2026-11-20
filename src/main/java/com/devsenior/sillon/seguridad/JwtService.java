package com.devsenior.sillon.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private static final long EXPIRACION_MINUTOS = 1440; // 24 horas

    private final SecretKey llave;
    private final Clock clock;

    public JwtService(@Value("${sillon.jwt.secreto}") String secreto, Clock clock) {
        this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String generarToken(UsuarioPrincipal usuario) {
        Instant ahora = Instant.now(clock);
        Instant expiracion = ahora.plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalonId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(llave)
                .compact();
    }

    public long getExpiraEnMinutos() {
        return EXPIRACION_MINUTOS;
    }

    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            return claims.getExpiration().after(Date.from(Instant.now(clock)));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(llave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
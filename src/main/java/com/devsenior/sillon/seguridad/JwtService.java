package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.usuario.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey llave;
    private final int expiraEnMinutos;
    private final Clock clock;

    public JwtService(@Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expira-en-minutos}") int expiraEnMinutos, Clock clock) {
        this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiraEnMinutos = expiraEnMinutos;
        this.clock = clock;
    }

    public int getExpiraEnMinutos() {
        return expiraEnMinutos;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expiracion = ahora.plus(expiraEnMinutos, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalon().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(llave)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload();
    }
}

package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class ServicioJwt {

    private static final int EXPIRA_EN_MINUTOS = 60;

    private final SecretKey clave;
    private final Clock reloj;

    public ServicioJwt(
            @Value("${sillon.jwt.secreto}") String secreto,
            Clock reloj) {

        this.clave = Keys.hmacShaKeyFor(
                secreto.getBytes(StandardCharsets.UTF_8));
        this.reloj = reloj;
    }

    public String generarToken(UsuarioPrincipal usuario) {
        Instant ahora = reloj.instant();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalonId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(
                        ahora.plus(EXPIRA_EN_MINUTOS, ChronoUnit.MINUTES)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .clock(() -> Date.from(reloj.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public int getExpiraEnMinutos() {
        return EXPIRA_EN_MINUTOS;
    }

}
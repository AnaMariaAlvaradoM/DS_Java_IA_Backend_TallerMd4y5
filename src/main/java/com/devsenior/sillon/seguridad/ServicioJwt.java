package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Emisión y validación de JWT (paso 2.2). El token lleva el correo como
 * sujeto, y el rol y el salón como datos adicionales. El secreto se lee de
 * la configuración; nunca va fijo en el código.
 */
@Service
public class ServicioJwt {

    private final SecretKey llave;
    private final long expiracionMinutos;
    private final Clock reloj;

    public ServicioJwt(
            @Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expiracion-minutos:1440}") long expiracionMinutos,
            Clock reloj) {
        this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
        this.reloj = reloj;
    }

    public String generarToken(String email, String rol, Long salonId) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .claim("salonId", salonId)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofMinutes(expiracionMinutos))))
                .signWith(llave)
                .compact();
    }

    /** Lanza JwtException si el token es inválido, corrupto o expiró. */
    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(llave)
                .clock(() -> Date.from(reloj.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerEmail(String token) {
        return validar(token).getSubject();
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}

package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.usuario.Rol;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServicio {

    public static final int MINUTOS_EXPIRACION = 60;

    private final SecretKey clave;
    private final Clock reloj;

    public JwtServicio(@Value("${jwt.secreto}") String secreto, Clock reloj) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.reloj = reloj;
    }

    public String generar(String email, Rol rol, Long salonId) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime vence = ahora.plusMinutes(MINUTOS_EXPIRACION);
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol.name())
                .claim("salonId", salonId)
                .issuedAt(Date.from(ahora.atZone(reloj.getZone()).toInstant()))
                .expiration(Date.from(vence.atZone(reloj.getZone()).toInstant()))
                .signWith(clave)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

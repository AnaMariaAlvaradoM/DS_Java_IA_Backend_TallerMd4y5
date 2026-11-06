
package com.devsenior.sillon.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class ServicioJwt {

    private final SecretKey clave;
    private final Clock reloj;
    private final Duration duracion;

    public ServicioJwt(
            @Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expiracion-horas:24}") long horas,
            Clock reloj) {

        this.clave = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secreto)
        );

        if (horas <= 0) {
            throw new IllegalArgumentException(
                    "La duración del JWT debe ser positiva"
            );
        }

        this.reloj = reloj;
        this.duracion = Duration.ofHours(horas);
    }

    public String generarToken(UsuarioPrincipal usuario) {

        Instant ahora = reloj.instant();

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalonId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    public Claims obtenerDatos(String token) {

        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String obtenerEmail(String token) {
        return obtenerDatos(token).getSubject();
    }

    public boolean validarToken(
            String token,
            UsuarioPrincipal usuario) {

        try {
            Claims datos = obtenerDatos(token);
            Object salonId = datos.get("salonId");

            return usuario.isEnabled()
                    && usuario.getUsername().equals(datos.getSubject())
                    && usuario.getRol().name().equals(
                            datos.get("rol", String.class))
                    && salonId instanceof Number numero
                    && numero.longValue() == usuario.getSalonId();

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

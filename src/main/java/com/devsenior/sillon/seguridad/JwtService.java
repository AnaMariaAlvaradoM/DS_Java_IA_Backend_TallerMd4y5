package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey clave;
    private final Clock reloj;
    private final long expiraEnMinutos;

    public JwtService(@Value("${sillon.jwt.secreto}") String secreto,
                      @Value("${sillon.jwt.expira-en-minutos:60}") long expiraEnMinutos,
                      Clock reloj) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiraEnMinutos = expiraEnMinutos;
        this.reloj = reloj;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalon().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiraEnMinutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return leer(token).getSubject();
    }

    public long getExpiraEnMinutos() { return expiraEnMinutos; }

    private Claims leer(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}

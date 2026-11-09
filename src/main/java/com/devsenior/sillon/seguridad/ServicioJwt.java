package com.devsenior.sillon.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ServicioJwt {
    private static final long EXPIRACION_MINUTOS = 60;
    private final SecretKey clave;
    private final Clock clock;

    public ServicioJwt(@Value("${sillon.jwt.secreto}") String secreto, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public long getExpiraEnMinutos() { return EXPIRACION_MINUTOS; }

    public String generar(UsuarioPrincipal usuario) {
        Date ahora = Date.from(clock.instant());
        return Jwts.builder().subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalonId())
                .issuedAt(ahora)
                .expiration(Date.from(clock.instant().plus(Duration.ofMinutes(EXPIRACION_MINUTOS))))
                .signWith(clave).compact();
    }

    public Claims validar(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}

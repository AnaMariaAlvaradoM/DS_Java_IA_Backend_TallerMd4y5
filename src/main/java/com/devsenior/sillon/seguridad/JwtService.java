package com.devsenior.sillon.seguridad;

import com.devsenior.sillon.usuario.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMinutos;
    private final Clock clock;

    public JwtService(
            @Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expiracion-minutos:1440}") long expiracionMinutos,
            Clock clock
    ) {
        byte[] keyBytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiracionMinutos = expiracionMinutos;
        this.clock = clock;
    }

    public String generarToken(String email, Rol rol, Long salonId) {
        long ahoraMillis = clock.millis();
        Date expedido = new Date(ahoraMillis);
        Date expira = new Date(ahoraMillis + (expiracionMinutos * 60 * 1000));

        return Jwts.builder()
                .subject(email)
                .claim("rol", rol.name())
                .claim("salonId", salonId)
                .issuedAt(expedido)
                .expiration(expira)
                .signWith(key)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date(clock.millis()));
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}

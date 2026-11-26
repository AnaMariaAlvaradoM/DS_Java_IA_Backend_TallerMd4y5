package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.usuario.Usuario;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Crea y valida tokens. Sujeto: el correo. Datos adicionales: rol y salonId. */
@Service
public class JwtService {

    private final SecretKey llave;
    private final long expiraMinutos;
    private final Clock clock;

    public JwtService(@Value("${sillon.jwt.secreto}") String secreto,
            @Value("${sillon.jwt.expira-minutos:120}") long expiraMinutos, Clock clock) {
        this.llave = Keys.hmacShaKeyFor(aBytesSeguros(secreto));
        this.expiraMinutos = expiraMinutos;
        this.clock = clock;
    }

    public String generar(Usuario usuario) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalon().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiraMinutos * 60)))
                .signWith(llave)
                .compact();
    }

    /** Devuelve el correo del token, o null si el token es inválido o venció. */
    public String extraerEmail(String token) {
        try {
            return Jwts.parser().verifyWith(llave).build().parseSignedClaims(token).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpiraMinutos() {
        return expiraMinutos;
    }

    /** HMAC-SHA256 exige mínimo 32 bytes: si el secreto es más corto, se deriva con SHA-256. */
    private static byte[] aBytesSeguros(String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length >= 32) {
            return bytes;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

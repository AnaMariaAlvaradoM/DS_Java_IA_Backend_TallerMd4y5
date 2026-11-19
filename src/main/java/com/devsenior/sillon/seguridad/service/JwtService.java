package com.devsenior.sillon.seguridad.service;

import com.devsenior.sillon.seguridad.config.JwtProperties;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Firma y valida los JWT. Sujeto: el correo. Datos adicionales: rol y salonId. */
@Service
public class JwtService {

    private final SecretKey llave;
    private final long expiracionMinutos;
    private final Clock clock;

    public JwtService(JwtProperties propiedades, Clock clock) {
        byte[] bytes = propiedades.secreto().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("sillon.jwt.secreto debe tener al menos 32 caracteres");
        }
        this.llave = Keys.hmacShaKeyFor(bytes);
        this.expiracionMinutos = propiedades.expiracionMinutos();
        this.clock = clock;
    }

    public String generar(UsuarioPrincipal usuario) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .claim("salonId", usuario.getSalonId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusSeconds(expiracionMinutos * 60)))
                .signWith(llave)
                .compact();
    }

    /** Devuelve el correo del token si es válido; vacío si es inválido o expiró. */
    public Optional<String> correoSiEsValido(String token) {
        try {
            String correo = Jwts.parser()
                    .verifyWith(llave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(correo);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}

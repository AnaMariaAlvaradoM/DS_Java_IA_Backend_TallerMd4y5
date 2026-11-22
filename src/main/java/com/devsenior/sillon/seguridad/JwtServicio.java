package com.devsenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.devsenior.sillon.usuario.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Crea y valida los tokens JWT. Escrito para jjwt 0.11.5 (la version del pom):
 * en esa version los metodos se llaman setSubject / setExpiration / parserBuilder().setSigningKey(...).
 * (En jjwt 0.12 o superior se llaman subject / expiration / parser().verifyWith(...): no mezclar versiones.)
 *
 * El token lleva el correo como sujeto, y el rol y el salon como datos adicionales (claims).
 * El rol del token es informativo: la autoridad real se lee de la base de datos en cada peticion,
 * asi un usuario desactivado o con otro rol pierde el acceso sin esperar a que el token venza.
 */
@Service
public class JwtServicio {

    static final String CLAIM_ROL = "rol";
    static final String CLAIM_SALON_ID = "salonId";

    private final SecretKey clave;
    private final long expiracionMinutos;
    private final Clock reloj;

    public JwtServicio(JwtConfig config, Clock reloj) {
        // hmacShaKeyFor exige al menos 32 bytes; con un secreto mas corto lanza WeakKeyException al arrancar.
        this.clave = Keys.hmacShaKeyFor(config.secreto().getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = config.expiracionMinutos();
        this.reloj = reloj;
    }

    public String generar(Usuario usuario) {
        Instant ahora = reloj.instant();
        return Jwts.builder()
                .setSubject(usuario.getEmail())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim(CLAIM_SALON_ID, usuario.getSalon().getId())
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(ahora.plus(Duration.ofMinutes(expiracionMinutos))))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve el correo del token solo si la firma es correcta y no ha vencido.
     * Cualquier otro caso (firma falsa, token alterado, vencido, basura) da vacio: no se lanza nada.
     */
    public Optional<String> extraerEmail(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(clave)
                    // El vencimiento se compara con el reloj del proyecto, no con el del sistema.
                    .setClock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }
}

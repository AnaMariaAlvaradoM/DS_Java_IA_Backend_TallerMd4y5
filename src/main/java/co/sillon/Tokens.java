package co.sillon;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class Tokens {
    private final SecretKey clave;
    private final Clock clock;
    Tokens(@Value("${sillon.jwt.secreto}") String secreto, Clock clock) {
        this.clock=clock;
        var bytes=secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length==0) { bytes=new byte[32]; new SecureRandom().nextBytes(bytes); }
        if (bytes.length<32) throw new IllegalArgumentException("JWT_SECRETO requiere 32 bytes o más");
        clave=Keys.hmacShaKeyFor(bytes);
    }
    String emitir(Usuario u) {
        var ahora=clock.instant();
        return Jwts.builder().issuer("sillon").subject(u.email).claim("rol",u.rol).claim("salonId",u.salon.id)
                .issuedAt(Date.from(ahora)).expiration(Date.from(ahora.plus(Duration.ofHours(8))))
                .signWith(clave).compact();
    }
    String correo(String token) {
        return Jwts.parser().verifyWith(clave).requireIssuer("sillon").clock(() -> Date.from(clock.instant()))
                .build().parseSignedClaims(token).getPayload().getSubject();
    }
}

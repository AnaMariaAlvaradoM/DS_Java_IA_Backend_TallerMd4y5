package com.devSenior.sillon.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.devSenior.sillon.usuario.entidad.Rol;

@Service
public class JwtServicio {

	private final SecretKey clave;
	private final int expiraEnMinutos;
	private final Clock reloj;

	public JwtServicio(
			@Value("${sillon.jwt.secreto}") String secreto,
			@Value("${sillon.jwt.expiracion-minutos}") int expiraEnMinutos,
			Clock reloj) {
		byte[] bytes = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException("JWT_SECRETO debe tener al menos 32 bytes");
		}
		this.clave = Keys.hmacShaKeyFor(bytes);
		this.expiraEnMinutos = expiraEnMinutos;
		this.reloj = reloj;
	}

	public String generar(String email, Rol rol, Long salonId) {
		Date ahora = Date.from(reloj.instant());
		Date expira = Date.from(reloj.instant().plusSeconds(expiraEnMinutos * 60L));
		return Jwts.builder()
				.subject(email)
				.claim("rol", rol.name())
				.claim("salonId", salonId)
				.issuedAt(ahora)
				.expiration(expira)
				.signWith(clave)
				.compact();
	}

	public String extraerCorreo(String token) {
		return Jwts.parser()
				.verifyWith(clave)
				.clock(() -> Date.from(reloj.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getSubject();
	}

	public int getExpiraEnMinutos() {
		return expiraEnMinutos;
	}

}

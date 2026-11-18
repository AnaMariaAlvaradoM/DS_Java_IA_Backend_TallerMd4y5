package com.devsenior.sillon.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class ServicioJwtTest {

    private static final String SECRETO = "secreto-de-pruebas-con-mas-de-32-caracteres-para-hs256";
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-10-20T15:30:00Z");

    private ServicioJwt servicioJwt;

    @BeforeEach
    void configurar() {
        servicioJwt = new ServicioJwt(SECRETO, 60, Clock.fixed(AHORA, ZONA));
    }

    @Test
    void elTokenLlevaElCorreoComoSujetoYElRolYElSalonComoDatos() {
        String token = servicioJwt.generarToken("camilo@navaja.co", "DUENO", 7L);

        Claims claims = servicioJwt.validar(token);
        assertThat(claims.getSubject()).isEqualTo("camilo@navaja.co");
        assertThat(claims.get("rol", String.class)).isEqualTo("DUENO");
        assertThat(claims.get("salonId", Long.class)).isEqualTo(7L);
    }

    @Test
    void extraerEmailDevuelveElSujetoDelToken() {
        String token = servicioJwt.generarToken("sara@navaja.co", "ESTILISTA", 7L);

        assertThat(servicioJwt.extraerEmail(token)).isEqualTo("sara@navaja.co");
    }

    @Test
    void unTokenCorruptoNoEsValido() {
        assertThatThrownBy(() -> servicioJwt.validar("token.corrupto.xyz"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenExpiradoNoEsValido() {
        String token = servicioJwt.generarToken("camilo@navaja.co", "DUENO", 7L);

        ServicioJwt validadorEnElFuturo = new ServicioJwt(SECRETO, 60,
                Clock.fixed(AHORA.plus(Duration.ofMinutes(61)), ZONA));

        assertThatThrownBy(() -> validadorEnElFuturo.validar(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}

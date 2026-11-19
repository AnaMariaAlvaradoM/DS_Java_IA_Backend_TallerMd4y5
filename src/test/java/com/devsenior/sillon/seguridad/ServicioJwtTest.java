package com.devsenior.sillon.seguridad;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.usuario.modelo.Rol;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;

class ServicioJwtTest {

    private static final String SECRETO_PRUEBA =
            "clave-ficticia-exclusiva-para-pruebas-unitarias-jwt";

    private Clock reloj;
    private ServicioJwt servicioJwt;
    private UsuarioPrincipal usuario;

    @BeforeEach
    void preparar() {

        reloj = Clock.fixed(
                Instant.parse("2026-11-19T15:00:00Z"),
                ZoneId.of("America/Bogota"));

        servicioJwt = new ServicioJwt(SECRETO_PRUEBA, reloj);

        usuario = mock(UsuarioPrincipal.class);

        when(usuario.getUsername())
                .thenReturn("camilo@pruebas.example");

        when(usuario.getRol()).thenReturn(Rol.DUENO);
        when(usuario.getSalonId()).thenReturn(1L);
    }

    @Test
    void generarYValidarTokenCorrectamente() {

        String token = servicioJwt.generarToken(usuario);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims datos = servicioJwt.validarToken(token);

        assertNotNull(datos);
        assertEquals(
                "camilo@pruebas.example",
                datos.getSubject());
    }

    @Test
    void tokenContieneRolSalonYExpiracionCorrectos() {

        String token = servicioJwt.generarToken(usuario);

        Claims datos = servicioJwt.validarToken(token);

        assertEquals("DUENO", datos.get("rol", String.class));
        assertEquals(1L, datos.get("salonId", Long.class));

        assertEquals(
                Date.from(reloj.instant()),
                datos.getIssuedAt());

        assertEquals(
                Date.from(reloj.instant().plus(Duration.ofMinutes(60))),
                datos.getExpiration());

        assertEquals(60, servicioJwt.getExpiraEnMinutos());
    }

    @Test
    void validarTokenRechazaTokenInvalido() {

        assertThrows(
                JwtException.class,
                () -> servicioJwt.validarToken("token-invalido"));
    }

    @Test
    void validarTokenRechazaTokenExpirado() {

        String token = servicioJwt.generarToken(usuario);

        Clock relojPosterior = Clock.offset(
                reloj,
                Duration.ofMinutes(61));

        ServicioJwt servicioPosterior =
                new ServicioJwt(SECRETO_PRUEBA, relojPosterior);

        assertThrows(
                ExpiredJwtException.class,
                () -> servicioPosterior.validarToken(token));
    }
}
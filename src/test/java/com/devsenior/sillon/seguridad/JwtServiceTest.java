package com.devsenior.sillon.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRETO_PRUEBA = "secreto-ficticio-exclusivo-para-tests-0123456789";
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneId.of("America/Bogota"));
    private final JwtService servicio = new JwtService(SECRETO_PRUEBA, 60, reloj);

    @Test
    void tokenFirmadoIdentificaAlUsuario() {
        assertEquals("dueno@ejemplo.test", servicio.obtenerEmail(servicio.generar(usuario())));
    }

    @Test
    void tokenVencidoNoAutentica() {
        String token = servicio.generar(usuario());
        Clock despues = Clock.offset(reloj, java.time.Duration.ofMinutes(61));
        JwtService verificador = new JwtService(SECRETO_PRUEBA, 60, despues);
        assertThrows(ExpiredJwtException.class, () -> verificador.obtenerEmail(token));
    }

    @Test
    void firmaDeOtraClaveNoAutentica() {
        String token = servicio.generar(usuario());
        JwtService otraClave = new JwtService("otra-clave-ficticia-para-tests-distinta-0123456789", 60, reloj);
        assertThrows(JwtException.class, () -> otraClave.obtenerEmail(token));
    }

    private Usuario usuario() {
        Salon salon = new Salon();
        salon.setId(10L);
        Usuario usuario = new Usuario();
        usuario.setEmail("dueno@ejemplo.test");
        usuario.setRol(Rol.DUENO);
        usuario.setSalon(salon);
        return usuario;
    }
}

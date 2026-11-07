
package com.devsenior.sillon.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.usuario.Rol;

class ServicioJwtTest {

    @Test
    void debeRechazarTokenVencido() {

        // Clave exclusiva para la prueba, no para producción
        String secreto = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8)
        );

        // Simular una fecha antigua de emisión
        Clock reloj = Clock.fixed(
                Instant.parse("2020-01-01T10:00:00Z"),
                ZoneOffset.UTC
        );

        // Crear el servicio con tokens de una hora
        ServicioJwt servicioJwt = new ServicioJwt(
                secreto,
                1,
                reloj
        );

        // Simular un usuario autenticado
        UsuarioPrincipal usuario = mock(UsuarioPrincipal.class);

        when(usuario.getUsername()).thenReturn("prueba@example.com");
        when(usuario.getRol()).thenReturn(Rol.DUENO);
        when(usuario.getSalonId()).thenReturn(1L);

        // Generar un token que ya estará vencido
        String token = servicioJwt.generarToken(usuario);

        // Comprobar que el token sea rechazado
        boolean resultado = servicioJwt.validarToken(token, usuario);

        assertFalse(resultado);
    }
}


package com.devsenior.sillon.ia;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AsistenteIATest {

    private final ProveedorFormatoOpenAI principal =
            mock(ProveedorFormatoOpenAI.class);

    private final ProveedorClaude respaldo =
            mock(ProveedorClaude.class);

    private final AsistenteIA asistente =
            new AsistenteIA(principal, respaldo);

    @Test
    void utilizarGeminiCuandoFunciona() {

        ProveedorIA.Resultado resultado =
                new ProveedorIA.Resultado(
                        "Respuesta de Gemini",
                        null
                );

        when(principal.generar("instrucciones", "mensaje"))
                .thenReturn(resultado);

        ProveedorIA.Resultado respuesta =
                asistente.generar("instrucciones", "mensaje");

        assertEquals(
                "Respuesta de Gemini",
                respuesta.contenido()
        );

        verifyNoInteractions(respaldo);
    }

    @Test
    void utilizarClaudeCuandoGeminiFalla() {

        when(principal.generar("instrucciones", "mensaje"))
                .thenThrow(
                        new IllegalStateException(
                                "Gemini no disponible"
                        )
                );

        when(respaldo.generar("instrucciones", "mensaje"))
                .thenReturn(
                        new ProveedorIA.Resultado(
                                "Respuesta de Claude",
                                null
                        )
                );

        ProveedorIA.Resultado respuesta =
                asistente.generar("instrucciones", "mensaje");

        assertEquals(
                "Respuesta de Claude",
                respuesta.contenido()
        );
    }

    @Test
    void devolver503CuandoAmbosProveedoresFallan() {

        when(principal.generar("instrucciones", "mensaje"))
                .thenThrow(
                        new IllegalStateException(
                                "Gemini no disponible"
                        )
                );

        when(respaldo.generar("instrucciones", "mensaje"))
                .thenThrow(
                        new IllegalStateException(
                                "Claude no configurado"
                        )
                );

        ResponseStatusException excepcion =
                assertThrows(
                        ResponseStatusException.class,
                        () -> asistente.generar(
                                "instrucciones",
                                "mensaje"
                        )
                );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                excepcion.getStatusCode()
        );
    }
}

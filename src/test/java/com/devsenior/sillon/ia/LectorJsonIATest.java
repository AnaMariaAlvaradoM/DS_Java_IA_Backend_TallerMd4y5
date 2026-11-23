
package com.devsenior.sillon.ia;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class LectorJsonIATest {

    private final LectorJsonIA lector =
            new LectorJsonIA(JsonMapper.builder().build());

    @Test
    void leerRespuestaValida() {
        var datos = lector.leerObjeto(
                """
                {
                    "respuesta": "Hola, bienvenido",
                    "escalar": false
                }
                """
        );

        assertEquals(
                "Hola, bienvenido",
                lector.leerTextoObligatorio(datos, "respuesta")
        );

        assertFalse(
                lector.leerBooleanoObligatorio(datos, "escalar")
        );
    }

    @Test
    void aceptarJsonConBloqueMarkdown() {
        var datos = lector.leerObjeto(
                """
                ```json
                {"mensaje": "Cita confirmada"}
                ```
                """
        );

        assertEquals(
                "Cita confirmada",
                lector.leerTextoObligatorio(datos, "mensaje")
        );
    }

    @Test
    void rechazarJsonInvalido() {
        assertThrows(
                IllegalStateException.class,
                () -> lector.leerObjeto("{respuesta:")
        );
    }

    @Test
    void rechazarCampoObligatorioAusente() {
        var datos = lector.leerObjeto("{}");

        assertThrows(
                IllegalStateException.class,
                () -> lector.leerTextoObligatorio(
                        datos,
                        "respuesta"
                )
        );
    }
}

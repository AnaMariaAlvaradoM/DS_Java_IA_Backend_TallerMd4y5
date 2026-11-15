
package com.devsenior.sillon.cliente;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TelefonoUtilsTest {

    @Test
    void normalizarTelefonoConEspacios() {
        String resultado = TelefonoUtils.normalizar("310 555 1234");

        assertEquals("3105551234", resultado);
    }

    @Test
    void normalizarTelefonoConGuiones() {
        String resultado = TelefonoUtils.normalizar("310-555-1234");

        assertEquals("3105551234", resultado);
    }

    @Test
    void conservarPrefijoInternacional() {
        String resultado = TelefonoUtils.normalizar("+57 (310) 555-1234");

        assertEquals("+573105551234", resultado);
    }

    @Test
    void manejarTelefonoNulo() {
        String resultado = TelefonoUtils.normalizar(null);

        assertEquals("", resultado);
    }
}

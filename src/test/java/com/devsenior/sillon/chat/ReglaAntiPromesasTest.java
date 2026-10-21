package com.devsenior.sillon.chat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReglaAntiPromesasTest {
    private final ReglaAntiPromesas regla = new ReglaAntiPromesas();
    @ParameterizedTest @ValueSource(strings = {"descuento", "CUPÓN", "cupon", "GRATIS", "Promoción", "PROMOCION", "reembolsaremos"})
    void bloqueaTodosLosTerminosSinDistinguirMayusculas(String palabra) {
        assertTrue(regla.bloquear("Su " + palabra + " está disponible"));
    }
    @Test void falsoPositivoSeBloqueaYMensajeFijoEsSeguro() {
        assertTrue(regla.bloquear("No ofrecemos descuentos"));
        assertFalse(regla.bloquear(ReglaAntiPromesas.MENSAJE_SEGURO));
        assertFalse(regla.bloquear("El corte cuesta $35.000"));
    }
}

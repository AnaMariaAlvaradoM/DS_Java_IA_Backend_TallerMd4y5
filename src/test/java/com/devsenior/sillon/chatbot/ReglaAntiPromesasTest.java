package com.devsenior.sillon.chatbot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReglaAntiPromesasTest {

    @Test
    @DisplayName("Debe detectar palabras clave con mayúsculas y tilde (CUPÓN)")
    void detectaCuponConMayusculaYTilde() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("¡Aprovecha nuestro CUPÓN de bienvenida!");
        assertTrue(resultado, "Debe detectar 'CUPÓN' independientemente de las mayúsculas o tildes");
    }

    @Test
    @DisplayName("Debe detectar palabras clave en minúsculas y sin tilde (cupon)")
    void detectaCuponSinTilde() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("Aplica el cupon al pagar");
        assertTrue(resultado, "Debe detectar 'cupon' sin tilde");
    }

    @Test
    @DisplayName("Debe detectar términos prohibidos separados por salto de línea")
    void detectaDescuentoConSaltoDeLinea() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("Hola\nte doy un descuento especial");
        assertTrue(resultado, "Debe detectar 'descuento' incluso si hay saltos de línea \\n en el texto");
    }

    @Test
    @DisplayName("Debe detectar el código promocional SILLA50")
    void detectaSilla50() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("Usa el código SILLA50 para tu cita");
        assertTrue(resultado, "Debe detectar el código de cupón 'SILLA50'");
    }

    @Test
    @DisplayName("Debe detectar derivados de reembolso por coincidencia de prefijo (reembols)")
    void detectaReembolso() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("Solicite un reembolso de su dinero");
        assertTrue(resultado, "Debe detectar la raíz 'reembols' en la palabra 'reembolso'");
    }

    @Test
    @DisplayName("Debe retornar false para textos limpios sin promesas ni cupones")
    void retornaFalseParaTextoLimpio() {
        boolean resultado = ReglaAntiPromesas.contienePromesa("Atendemos de lunes a sábado de 8:00 a 19:00.");
        assertFalse(resultado, "No debe activar la regla para textos informativos comunes");
    }

    @Test
    @DisplayName("Debe retornar false para valores null o cadenas vacías")
    void retornaFalseParaNullOYVacio() {
        assertFalse(ReglaAntiPromesas.contienePromesa(null), "Debe retornar false si el texto es null");
        assertFalse(ReglaAntiPromesas.contienePromesa(""), "Debe retornar false si el texto está vacío");
        assertFalse(ReglaAntiPromesas.contienePromesa("   "), "Debe retornar false si el texto solo tiene espacios");
    }

    @Test
    @DisplayName("El mensaje seguro configurado no debe activar la propia regla anti-promesas")
    void mensajeSeguroNoActivaLaRegla() {
        boolean resultado = ReglaAntiPromesas.contienePromesa(ReglaAntiPromesas.MENSAJE_SEGURO);
        assertFalse(resultado, "El mensaje seguro por defecto no debe contener palabras prohibidas");
    }
}

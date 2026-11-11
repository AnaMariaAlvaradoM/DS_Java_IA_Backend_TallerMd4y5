package com.devsenior.sillon.chatbot;

import java.util.List;
import java.util.Locale;

public class ReglaAntiPromesas {

    public static final String MENSAJE_SEGURO =
            "Gracias por escribirnos. Una persona de nuestro equipo se pondrá en contacto con usted para ayudarle con su solicitud.";

    private static final List<String> TERMINOS_PROHIBIDOS = List.of(
            "descuento",
            "cupón",
            "cupon",
            "gratis",
            "promoción",
            "promocion",
            "reembols",
            "silla50"
    );

    public static boolean contienePromesa(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        String textoNormalizado = texto.toLowerCase(Locale.ROOT);
        return TERMINOS_PROHIBIDOS.stream().anyMatch(textoNormalizado::contains);
    }
}

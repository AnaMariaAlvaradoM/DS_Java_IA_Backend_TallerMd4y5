package com.devsenior.sillon.chat;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ReglaAntiPromesas {
    public static final String MENSAJE_SEGURO = "Para atender su solicitud, una persona del equipo se pondrá en contacto con usted.";
    private static final List<String> TERMINOS = List.of("descuento", "cupón", "cupon", "gratis", "promoción", "promocion", "reembols");

    public boolean bloquear(String respuesta) {
        String texto = respuesta.toLowerCase(Locale.ROOT);
        return TERMINOS.stream().anyMatch(texto::contains);
    }
}

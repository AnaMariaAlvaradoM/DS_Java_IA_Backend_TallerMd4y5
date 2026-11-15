
package com.devsenior.sillon.cliente;

public final class TelefonoUtils {

    private TelefonoUtils() {
    }

    public static String normalizar(String telefono) {
        if (telefono == null) {
            return "";
        }

        String limpio = telefono.trim();
        boolean tienePrefijo = limpio.startsWith("+");

        String digitos = limpio.replaceAll("[^0-9]", "");

        if (tienePrefijo && !digitos.isEmpty()) {
            return "+" + digitos;
        }

        return digitos;
    }
}

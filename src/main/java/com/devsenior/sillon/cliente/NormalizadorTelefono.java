package com.devsenior.sillon.cliente;

/**
 * Normaliza numeros de telefono conservando solo digitos y el signo +.
 */
public final class NormalizadorTelefono {

    private NormalizadorTelefono() {
    }

    public static String normalizar(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("[^0-9+]", "");
    }
}

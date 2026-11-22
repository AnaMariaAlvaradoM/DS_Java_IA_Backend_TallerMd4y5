package com.devsenior.sillon.usuario;

import java.util.Locale;

/** El correo se guarda y se busca siempre en minusculas y sin espacios. Lo usan el registro, el login y los estilistas. */
public final class NormalizadorCorreo {

    private NormalizadorCorreo() {
    }

    public static String normalizar(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }
}

package com.devsenior.sillon.salon.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NO_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern ESPACIOS = Pattern.compile("[\\s]+");
    private static final Pattern GUIONES_DUPLICADOS = Pattern.compile("-+");

    private SlugUtil() {
    }

    public static String generarSlug(String entrada) {
        if (entrada == null) {
            return "";
        }
        String sinEspacios = ESPACIOS.matcher(entrada.trim()).replaceAll("-");
        String normalizado = Normalizer.normalize(sinEspacios, Normalizer.Form.NFD);
        String sinAcentos = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalizado).replaceAll("");
        String soloLatin = NO_LATIN.matcher(sinAcentos).replaceAll("");
        String resultado = GUIONES_DUPLICADOS.matcher(soloLatin).replaceAll("-")
                .toLowerCase(Locale.ROOT);

        return resultado.replaceAll("^-|-$", "");
    }
}
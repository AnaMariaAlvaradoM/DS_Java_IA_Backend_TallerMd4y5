package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Convierte un texto en slug. No toca la base de datos, asi se prueba sin mocks.
 * "Barbería La Navaja" -> "barberia-la-navaja".
 */
public final class NormalizadorSlug {

    /** Si el nombre no tiene ni una letra ni un numero (por ejemplo "!!!"), el slug no puede quedar vacio. */
    static final String SLUG_POR_DEFECTO = "salon";

    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");
    private static final Pattern NO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern GUIONES_EN_LOS_EXTREMOS = Pattern.compile("^-+|-+$");

    private NormalizadorSlug() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return SLUG_POR_DEFECTO;
        }
        // 1. NFD separa la letra de su tilde ("é" -> "e" + marca) y se borran las marcas.
        String sinTildes = MARCAS_DE_ACENTO.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        // 2. Minusculas, y todo lo que no sea letra o numero se vuelve un guion (varios seguidos, uno solo).
        String conGuiones = NO_ALFANUMERICO.matcher(sinTildes.toLowerCase(Locale.ROOT)).replaceAll("-");
        // 3. Sin guiones al inicio ni al final.
        String slug = GUIONES_EN_LOS_EXTREMOS.matcher(conGuiones).replaceAll("");
        return slug.isEmpty() ? SLUG_POR_DEFECTO : slug;
    }
}

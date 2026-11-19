package com.devsenior.sillon.compartido.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;

/**
 * Formatos en español de Colombia escritos a mano, sin depender del Locale del JDK.
 * Así se evita el espacio invisible que Java pone en "a. m." y el resultado es igual en cualquier servidor.
 */
public final class FormatoColombia {

    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    private FormatoColombia() {
    }

    /** 10:30 → "10:30 a. m."; 19:00 → "7:00 p. m.". */
    public static String hora(LocalTime hora) {
        int h = hora.getHour() % 12 == 0 ? 12 : hora.getHour() % 12;
        String sufijo = hora.getHour() < 12 ? "a. m." : "p. m.";
        return String.format(Locale.ROOT, "%d:%02d %s", h, hora.getMinute(), sufijo);
    }

    /** 2026-10-20 → "martes 20 de octubre". */
    public static String fecha(LocalDate fecha) {
        return DIAS[fecha.getDayOfWeek().getValue() - 1] + " " + fecha.getDayOfMonth()
                + " de " + MESES[fecha.getMonthValue() - 1];
    }

    /** 35000 → "$35.000". */
    public static String pesos(long valor) {
        return "$" + String.format(Locale.ROOT, "%,d", valor).replace(',', '.');
    }
}

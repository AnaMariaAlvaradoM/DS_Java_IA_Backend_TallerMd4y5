package com.devsenior.sillon.salon;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FormatoHorarioSalon {
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-CO"));

    public String describir(ConfiguracionSalon configuracion) {
        String dias = configuracion.isAbreDomingos() ? "Lunes a domingo" : "Lunes a sábado (domingos cerrado)";
        return normalizarEspacios(dias + ", de " + configuracion.getHoraApertura().format(HORA)
                + " a " + configuracion.getHoraCierre().format(HORA));
    }

    private String normalizarEspacios(String texto) {
        return texto.replace('\u00a0', ' ').replace('\u202f', ' ');
    }
}

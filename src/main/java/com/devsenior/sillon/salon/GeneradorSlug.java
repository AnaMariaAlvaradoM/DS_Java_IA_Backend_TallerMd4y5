package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** "Barbería La Navaja" → "barberia-la-navaja"; si ya existe, "-2", "-3"... */
@Component
public class GeneradorSlug {

    private final SalonRepository salonRepository;

    public GeneradorSlug(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generarUnico(String nombre) {
        String base = normalizar(nombre);
        if (base.isEmpty()) {
            base = "salon";
        }
        String candidato = base;
        int numero = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + numero;
            numero++;
        }
        return candidato;
    }

    /** Sin tildes, minúsculas, lo que no sea letra o número pasa a guion, sin guiones en los extremos. */
    public static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}

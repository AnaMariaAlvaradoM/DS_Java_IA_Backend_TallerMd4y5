package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class GeneradorSlug {
    private final SalonRepository salones;

    public GeneradorSlug(SalonRepository salones) {
        this.salones = salones;
    }

    public String generar(String nombre) {
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "salon";
        }
        String candidato = base;
        for (int numero = 2; salones.existsBySlug(candidato); numero++) {
            candidato = base + "-" + numero;
        }
        return candidato;
    }
}

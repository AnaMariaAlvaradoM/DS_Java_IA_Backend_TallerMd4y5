
package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class GeneradorSlug {

    private final SalonRepository salonRepository;

    public GeneradorSlug(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generar(String nombre) {

        String slugBase = Normalizer
                .normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        String slug = slugBase;
        int numero = 2;

        while (salonRepository.existsBySlug(slug)) {
            slug = slugBase + "-" + numero;
            numero++;
        }

        return slug;
    }
}

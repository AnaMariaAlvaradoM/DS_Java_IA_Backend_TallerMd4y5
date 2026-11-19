package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.salon.repository.SalonRepository;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Identificador del salón para su URL pública.
 * "Barbería La Navaja" → "barberia-la-navaja"; si ya existe: "-2", "-3"...
 */
@Component
public class GeneradorSlug {

    private final SalonRepository salonRepository;

    public GeneradorSlug(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generar(String nombre) {
        String base = normalizar(nombre);
        String slug = base;
        int numero = 2;
        while (salonRepository.existsBySlug(slug)) {
            slug = base + "-" + numero;
            numero++;
        }
        return slug;
    }

    static String normalizar(String nombre) {
        String sinTildes = Normalizer.normalize(nombre == null ? "" : nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String base = sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return base.isEmpty() ? "salon" : base;
    }
}

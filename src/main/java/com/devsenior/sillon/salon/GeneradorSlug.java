package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Genera el identificador público del salón desde su nombre: sin tildes, en
 * minúsculas, con guiones, y con sufijo numérico si ya existe (paso 1.4).
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
        int sufijo = 2;
        while (salonRepository.existsBySlug(slug)) {
            slug = base + "-" + sufijo;
            sufijo++;
        }
        return slug;
    }

    /** "Barbería La Navaja" -> "barberia-la-navaja". */
    String normalizar(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}

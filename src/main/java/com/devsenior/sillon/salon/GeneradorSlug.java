package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class GeneradorSlug {
    private final SalonRepositorio salones;

    public GeneradorSlug(SalonRepositorio salones) { this.salones = salones; }

    public String generar(String nombre) {
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.isEmpty()) { throw new IllegalArgumentException("El nombre debe contener letras o números"); }
        String slug = base;
        for (int sufijo = 2; salones.existsBySlug(slug); sufijo++) {
            slug = base + "-" + sufijo;
        }
        return slug;
    }
}

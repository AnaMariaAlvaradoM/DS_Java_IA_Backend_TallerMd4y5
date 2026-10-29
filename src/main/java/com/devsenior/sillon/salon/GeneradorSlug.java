package com.devsenior.sillon.salon;

import java.text.Normalizer;

import org.springframework.stereotype.Component;

@Component
public class GeneradorSlug {

    private final SalonRepository salonRepository;

    public GeneradorSlug(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generar(String nombre) {
        String base = normalizar(nombre);
        String candidato = base;
        int sufijo = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }

    private String normalizar(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String minusculas = sinTildes.toLowerCase();
        String conGuiones = minusculas.replaceAll("[^a-z0-9]+", "-");
        return conGuiones.replaceAll("^-+|-+$", "");
    }
}

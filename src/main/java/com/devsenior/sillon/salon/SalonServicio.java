package com.devsenior.sillon.salon;

import java.text.Normalizer;

import org.springframework.stereotype.Service;

@Service
public class SalonServicio {

    private final SalonRepository salonRepository;

    public SalonServicio(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generarSlug(String nombre) {
        String base = slugificar(nombre);
        String candidato = base;
        int sufijo = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }

    private String slugificar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String minusculas = sinTildes.toLowerCase();
        String conGuiones = minusculas.replaceAll("[^a-z0-9]+", "-");
        return conGuiones.replaceAll("^-+|-+$", "");
    }
}

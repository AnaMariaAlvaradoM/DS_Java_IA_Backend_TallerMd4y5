package com.devsenior.sillon.salon;

import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
public class SlugService {

    private final SalonRepository salonRepository;

    public SlugService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generarSlug(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            nombre = "salon";
        }

        // Quitar tildes y acentos
        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        String sinAcentos = normalizado.replaceAll("\\p{M}", "");

        // Pasar a minúsculas y reemplazar todo lo que no sea letra o número por guiones
        String baseSlug = sinAcentos.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (baseSlug.isBlank()) {
            baseSlug = "salon";
        }

        String candidato = baseSlug;
        int contador = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + contador;
            contador++;
        }

        return candidato;
    }
}

package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.salon.repository.SalonRepository;

import java.text.Normalizer;

public class GeneradorSlugService {
    private final SalonRepository salonRepository;

    // Inyección por constructor (cumple con las reglas de AGENTS.md)
    public GeneradorSlugService(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    /**
     * Genera un slug único a partir del nombre del salón.
     * Ejemplo: "Barbería La Navaja" -> "barberia-la-navaja"
     * Si ya existe, genera "barberia-la-navaja-2", "barberia-la-navaja-3", etc.
     */
    public String generarSlugUnico(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "";
        }

        // 1. Normalizar texto para remover tildes y caracteres diacríticos
        String textoSinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // 2. Pasar a minúsculas
        String textoMinuscula = textoSinTildes.toLowerCase();

        // 3. Reemplazar todo lo que no sea letra minúscula o número por un guion
        String textoLimpio = textoMinuscula.replaceAll("[^a-z0-9]+", "-");

        // 4. Remover guiones al inicio o al final del texto
        String slugBase = textoLimpio.replaceAll("^-+|-+$", "");

        // 5. Garantizar unicidad en la base de datos
        String slugFinal = slugBase;
        int contador = 1;

        while (salonRepository.existsBySlug(slugFinal)) {
            slugFinal = slugBase + "-" + contador;
            contador++;
        }

        return slugFinal;
    }
}

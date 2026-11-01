package com.devsenior.sillon.salon;


import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SlugService {
    private final SalonRepository salonRepository;

    public SlugService(SalonRepository salonRepository){
        this.salonRepository = salonRepository;
    }

    private static final Pattern DIACRITICOS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public String generarSlugUnico(String nombre){
        if (nombre == null || nombre.trim().isEmpty()){
            throw new IllegalArgumentException("El nombre del salon no puede estar vacio");
        }

        String normalizado = Normalizer.normalize(nombre, Normalizer.Form.NFD);
        String sinTildes = DIACRITICOS.matcher(normalizado).replaceAll("");

        String conGuiones = sinTildes.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");

        if (conGuiones.startsWith("-")) {
            conGuiones = conGuiones.substring(1);
        }
        if (conGuiones.endsWith("-")) {
            conGuiones = conGuiones.substring(0, conGuiones.length() - 1);
        }

        if (conGuiones.isEmpty()) {
            conGuiones = "salon";
        }

        String slugBase = conGuiones;
        String slugFinal = slugBase;
        int contador = 2;

        while (salonRepository.existsBySlug(slugFinal)) {
            slugFinal = slugBase + "-" + contador;
            contador++;
        }

        return slugFinal;

    }
}

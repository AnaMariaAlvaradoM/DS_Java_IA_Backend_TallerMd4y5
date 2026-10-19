package com.devsenior.sillon.salon;

import java.text.Normalizer;
import java.util.Locale;
import com.devsenior.sillon.compartido.excepcion.DatosInvalidosException;
import org.springframework.stereotype.Component;

@Component
public class GeneradorSlug {
    private final SalonRepository repositorio;

    public GeneradorSlug(SalonRepository repositorio) {
        this.repositorio = repositorio;
    }

    public String generar(String nombre) {
        String base = Normalizer.normalize(nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            throw new DatosInvalidosException("salonNombre: debe contener letras o números para generar su slug");
        }
        String candidato = base;
        int sufijo = 2;
        while (repositorio.existsBySlug(candidato)) {
            candidato = base + "-" + sufijo++;
        }
        return candidato;
    }
}

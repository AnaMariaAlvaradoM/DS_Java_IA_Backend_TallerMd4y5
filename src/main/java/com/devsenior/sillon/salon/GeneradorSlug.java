package com.devsenior.sillon.salon;

import org.springframework.stereotype.Component;

/**
 * Genera el slug unico de un salon. Si "barberia-la-navaja" ya existe, prueba "-2", "-3" y asi sucesivamente.
 * La restriccion unica de la tabla es la red de seguridad si dos registros simultaneos eligen el mismo slug.
 */
@Component
public class GeneradorSlug {

    private final SalonRepository salonRepository;

    public GeneradorSlug(SalonRepository salonRepository) {
        this.salonRepository = salonRepository;
    }

    public String generar(String nombre) {
        String base = NormalizadorSlug.normalizar(nombre);
        String candidato = base;
        int numero = 2;
        while (salonRepository.existsBySlug(candidato)) {
            candidato = base + "-" + numero;
            numero++;
        }
        return candidato;
    }
}

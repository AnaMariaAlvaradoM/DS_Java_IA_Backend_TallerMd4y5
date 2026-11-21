package com.devsenior.sillon.salon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class GeneradorSlugTest {

    @Test
    void normalizaNombreYBuscaElPrimerSufijoLibre() {
        SalonRepository salones = mock(SalonRepository.class);
        when(salones.existsBySlug("barberia-la-navaja")).thenReturn(true);
        when(salones.existsBySlug("barberia-la-navaja-2")).thenReturn(true);
        when(salones.existsBySlug("barberia-la-navaja-3")).thenReturn(false);

        String slug = new GeneradorSlug(salones).generar("  Barbería La Navaja!  ");

        assertEquals("barberia-la-navaja-3", slug);
    }
}

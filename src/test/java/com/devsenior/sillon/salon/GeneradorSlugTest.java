package com.devsenior.sillon.salon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class GeneradorSlugTest {
    private final SalonRepository repositorio = mock(SalonRepository.class);
    private final GeneradorSlug generador = new GeneradorSlug(repositorio);

    @Test
    void normalizaTildesMayusculasYSeparadores() {
        assertEquals("barberia-la-navaja", generador.generar("  Barbería -- La Navaja!  "));
    }

    @Test
    void primeraColisionAgregaDos() {
        when(repositorio.existsBySlug("salon-aurora")).thenReturn(true);
        assertEquals("salon-aurora-2", generador.generar("Salón Aurora"));
    }

    @Test
    void omiteSufijosQueYaExisten() {
        when(repositorio.existsBySlug("salon-aurora")).thenReturn(true);
        when(repositorio.existsBySlug("salon-aurora-2")).thenReturn(true);
        assertEquals("salon-aurora-3", generador.generar("Salón Aurora"));
    }
}

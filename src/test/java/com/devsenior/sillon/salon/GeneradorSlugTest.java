package com.devsenior.sillon.salon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneradorSlugTest {

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private GeneradorSlug generadorSlug;

    @ParameterizedTest
    @CsvSource({
            "Barbería La Navaja, barberia-la-navaja",
            "Salón Aurora, salon-aurora",
            "'  Salón & Spa!  ', salon-spa",
            "PELUQUERÍA ÉXITO 2000, peluqueria-exito-2000"
    })
    void normalizaSinTildesEnMinusculasYConGuiones(String nombre, String esperado) {
        when(salonRepository.existsBySlug(esperado)).thenReturn(false);

        assertThat(generadorSlug.generar(nombre)).isEqualTo(esperado);
    }

    @Test
    void agregaSufijoNumericoSiElSlugYaExiste() {
        when(salonRepository.existsBySlug("barberia-la-navaja")).thenReturn(true);
        when(salonRepository.existsBySlug("barberia-la-navaja-2")).thenReturn(false);

        assertThat(generadorSlug.generar("Barbería La Navaja")).isEqualTo("barberia-la-navaja-2");
    }

    @Test
    void incrementaElSufijoHastaEncontrarUnSlugLibre() {
        when(salonRepository.existsBySlug("barberia-la-navaja")).thenReturn(true);
        when(salonRepository.existsBySlug("barberia-la-navaja-2")).thenReturn(true);
        when(salonRepository.existsBySlug("barberia-la-navaja-3")).thenReturn(false);

        assertThat(generadorSlug.generar("Barbería La Navaja")).isEqualTo("barberia-la-navaja-3");
    }
}

package com.devsenior.sillon.salon.service;

import com.devsenior.sillon.salon.repository.SalonRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "Barbería La Navaja, barberia-la-navaja",
            "Salón Aurora, salon-aurora",
            "'  Peluquería  Ñandú!! ', peluqueria-nandu",
            "'¡¡¡', salon"
    })
    void normalizar_quitaTildesEspaciosYSimbolos(String nombre, String esperado) {
        assertEquals(esperado, GeneradorSlug.normalizar(nombre));
    }

    @Test
    void generar_slugLibre_loDevuelveSinNumero() {
        when(salonRepository.existsBySlug("barberia-la-navaja")).thenReturn(false);

        assertEquals("barberia-la-navaja", generadorSlug.generar("Barbería La Navaja"));
    }

    @Test
    void generar_slugOcupado_agregaElSiguienteNumero() {
        when(salonRepository.existsBySlug("barberia-la-navaja")).thenReturn(true);
        when(salonRepository.existsBySlug("barberia-la-navaja-2")).thenReturn(true);
        when(salonRepository.existsBySlug("barberia-la-navaja-3")).thenReturn(false);

        assertEquals("barberia-la-navaja-3", generadorSlug.generar("Barbería La Navaja"));
    }
}

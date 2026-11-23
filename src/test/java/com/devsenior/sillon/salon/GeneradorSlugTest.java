
package com.devsenior.sillon.salon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class GeneradorSlugTest {

    @Test
    void debeGenerarSlugSinTildesYConGuiones() {

        // Simular el repositorio de salones
        SalonRepository repositorio = mock(SalonRepository.class);

        when(repositorio.existsBySlug("barberia-la-navaja"))
                .thenReturn(false);

        // Crear el generador
        GeneradorSlug generador = new GeneradorSlug(repositorio);

        // Ejecutar el método
        String resultado = generador.generar("Barbería La Navaja");

        // Verificar el resultado esperado
        assertEquals("barberia-la-navaja", resultado);
    }


    @Test
    void debeAgregarNumeroCuandoSlugYaExiste() {

        SalonRepository repositorio = mock(SalonRepository.class);

    // El slug original ya existe en la base de datos
        when(repositorio.existsBySlug("barberia-la-navaja"))
            .thenReturn(true);

    // El slug con -2 todavía está disponible
        when(repositorio.existsBySlug("barberia-la-navaja-2"))
            .thenReturn(false);

        GeneradorSlug generador = new GeneradorSlug(repositorio);

        String resultado = generador.generar("Barbería La Navaja");

        assertEquals("barberia-la-navaja-2", resultado);
    }

}

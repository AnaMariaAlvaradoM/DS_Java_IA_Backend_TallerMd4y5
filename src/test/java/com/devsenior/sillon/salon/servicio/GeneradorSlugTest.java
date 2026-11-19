package com.devsenior.sillon.salon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class GeneradorSlugTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	@InjectMocks
	private GeneradorSlug generadorSlug;

	@ParameterizedTest
	@CsvSource({
			"'Barbería La Navaja', barberia-la-navaja",
			"BARBERÍA, barberia",
			"Peña, pena",
			"'Sala 2', sala-2",
			"'---Hola---', hola",
			"'Hola  Mundo', hola--mundo"
	})
	void normalizaElNombreSegunElContrato(String nombre, String slugEsperado) {
		when(salonRepositorio.existsBySlug(slugEsperado)).thenReturn(false);

		assertEquals(slugEsperado, generadorSlug.generar(nombre));
	}

	@Test
	void conservaElSlugBaseCuandoEstaLibre() {
		when(salonRepositorio.existsBySlug("barberia-la-navaja")).thenReturn(false);

		assertEquals("barberia-la-navaja", generadorSlug.generar("Barbería La Navaja"));
	}

	@Test
	void agregaSufijoDosCuandoElSlugBaseExiste() {
		when(salonRepositorio.existsBySlug("barberia-la-navaja")).thenReturn(true);
		when(salonRepositorio.existsBySlug("barberia-la-navaja-2")).thenReturn(false);

		assertEquals("barberia-la-navaja-2", generadorSlug.generar("Barbería La Navaja"));
	}

	@Test
	void agregaElSiguienteSufijoLibre() {
		when(salonRepositorio.existsBySlug("barberia-la-navaja")).thenReturn(true);
		when(salonRepositorio.existsBySlug("barberia-la-navaja-2")).thenReturn(true);
		when(salonRepositorio.existsBySlug("barberia-la-navaja-3")).thenReturn(false);

		assertEquals("barberia-la-navaja-3", generadorSlug.generar("Barbería La Navaja"));
	}

}

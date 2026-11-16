package com.devSenior.sillon.salon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class GeneradorSlugTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	private GeneradorSlug generadorSlug;

	@BeforeEach
	void preparar() {
		generadorSlug = new GeneradorSlug(salonRepositorio);
	}

	@Test
	void quitaTildesYPasaAMinusculas() {
		when(salonRepositorio.existsBySlug("jose-maria")).thenReturn(false);

		assertEquals("jose-maria", generadorSlug.generar("José María"));
	}

	@Test
	void convierteSimbolosEnGuiones() {
		when(salonRepositorio.existsBySlug("barberia-spa")).thenReturn(false);

		assertEquals("barberia-spa", generadorSlug.generar("Barbería & Spa!!"));
	}

	@Test
	void agregaSufijoSiElSlugYaExiste() {
		when(salonRepositorio.existsBySlug("sillon")).thenReturn(true);
		when(salonRepositorio.existsBySlug("sillon-2")).thenReturn(false);

		assertEquals("sillon-2", generadorSlug.generar("Sillón"));
	}

	@Test
	void sigueElSufijoHastaEncontrarUnoLibre() {
		when(salonRepositorio.existsBySlug("sillon")).thenReturn(true);
		when(salonRepositorio.existsBySlug("sillon-2")).thenReturn(true);
		when(salonRepositorio.existsBySlug("sillon-3")).thenReturn(false);

		assertEquals("sillon-3", generadorSlug.generar("Sillón"));
	}

}

package com.devSenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class CreditoIaServicioTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	@InjectMocks
	private CreditoIaServicio creditoIaServicio;

	@Test
	void verificaYConsumeDeFormaAtomica() {
		Salon salon = salon(10);
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		creditoIaServicio.verificar(1L);
		when(salonRepositorio.consumirCreditoSiHay(1L)).thenReturn(1);
		salon.setCreditosIa(9);
		assertEquals(9, creditoIaServicio.consumir(1L));
		verify(salonRepositorio).consumirCreditoSiHay(1L);
	}

	@Test
	void noConsumeSiNoHayCreditos() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(0)));
		assertThrows(PagoRequeridoException.class, () -> creditoIaServicio.verificar(1L));
		verify(salonRepositorio, never()).consumirCreditoSiHay(1L);

		when(salonRepositorio.consumirCreditoSiHay(1L)).thenReturn(0);
		assertThrows(PagoRequeridoException.class, () -> creditoIaServicio.consumir(1L));
	}

	private Salon salon(int creditos) {
		Salon salon = new Salon();
		salon.setId(1L);
		salon.setCreditosIa(creditos);
		return salon;
	}

}

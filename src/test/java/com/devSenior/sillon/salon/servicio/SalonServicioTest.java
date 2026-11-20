package com.devSenior.sillon.salon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.salon.dto.SalonRespuesta;
import com.devSenior.sillon.salon.dto.SolicitudConfiguracion;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class SalonServicioTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	@InjectMocks
	private SalonServicio salonServicio;

	@Test
	void rechazaAperturaPosteriorOIgualAlCierre() {
		SolicitudConfiguracion solicitud = new SolicitudConfiguracion(LocalTime.of(19, 0), LocalTime.of(8, 0), false, 30);

		assertThrows(ConflictoException.class, () -> salonServicio.actualizarConfiguracion(1L, solicitud));
	}

	@Test
	void actualizaLaConfiguracionDelSalonAutenticado() {
		Salon salon = salon();
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(salonRepositorio.save(salon)).thenReturn(salon);

		SalonRespuesta respuesta = salonServicio.actualizarConfiguracion(1L,
				new SolicitudConfiguracion(LocalTime.of(9, 0), LocalTime.of(18, 0), true, 15));

		assertEquals(true, respuesta.configuracion().abreDomingos());
		assertEquals(15, respuesta.configuracion().intervaloMinutos());
		assertEquals(LocalTime.of(9, 0), respuesta.configuracion().horaApertura());
		assertEquals(Plan.GRATIS, respuesta.plan());
	}

	private Salon salon() {
		Salon salon = new Salon();
		salon.setId(1L);
		salon.setNombre("Sillón");
		salon.setSlug("sillon");
		salon.setPlan(Plan.GRATIS);
		salon.setCreditosIa(10);
		salon.setConfiguracion(ConfiguracionSalon.porDefecto());
		salon.setFechaCreacion(LocalDateTime.of(2026, 11, 12, 10, 0));
		return salon;
	}

}

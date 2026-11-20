package com.devSenior.sillon.salon.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.salon.dto.SalonRespuesta;
import com.devSenior.sillon.salon.entidad.ConfiguracionSalon;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class SalonServicioAdminTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	@InjectMocks
	private SalonServicio salonServicio;

	@Test
	void listaTodosLosSalonesSinDatosInternosDePago() {
		when(salonRepositorio.findAll()).thenReturn(List.of(salon(1L, "A", "salon-a"), salon(2L, "B", "salon-b")));

		List<SalonRespuesta> salones = salonServicio.listarTodos();

		assertEquals(2, salones.size());
		assertEquals(1L, salones.get(0).id());
		assertEquals("salon-a", salones.get(0).slug());
		assertEquals(Plan.GRATIS, salones.get(0).plan());
		assertEquals(10, salones.get(0).creditosIa());
		assertEquals(2L, salones.get(1).id());
		assertEquals("salon-b", salones.get(1).slug());
	}

	private Salon salon(Long id, String nombre, String slug) {
		Salon salon = new Salon();
		salon.setId(id);
		salon.setNombre(nombre);
		salon.setSlug(slug);
		salon.setCiudad("Bogota");
		salon.setDireccion("Calle 1");
		salon.setTelefono("3001234567");
		salon.setPlan(Plan.GRATIS);
		salon.setCreditosIa(10);
		salon.setStripeSuscripcionId("sub_no_sale");
		salon.setConfiguracion(ConfiguracionSalon.porDefecto());
		salon.setFechaCreacion(LocalDateTime.of(2026, 11, 19, 10, 0));
		return salon;
	}

}

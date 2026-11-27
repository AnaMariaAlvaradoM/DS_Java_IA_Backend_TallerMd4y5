package com.devsenior.sillon.servicio.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.dto.RespuestaServicio;
import com.devsenior.sillon.servicio.dto.SolicitudServicio;
import com.devsenior.sillon.servicio.mapper.ServicioMapper;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;

class ServicioServicioTest {

	private ServicioRepositorio servicioRepositorio;
	private SalonRepositorio salonRepositorio;
	private ServicioServicio servicioServicio;
	private Salon salon;

	@BeforeEach
	void preparar() {
		servicioRepositorio = mock(ServicioRepositorio.class);
		salonRepositorio = mock(SalonRepositorio.class);
		servicioServicio = new ServicioServicio(
				servicioRepositorio,
				salonRepositorio,
				new ServicioMapper());

		salon = new Salon();
		salon.setId(1L);
		salon.setNombre("Barbería La Navaja");
	}

	@Test
	void crearAsignaSalonYActivoPorDefectoTrue() {
		SolicitudServicio solicitud = new SolicitudServicio(
				"Corte clásico",
				"Corte con tijera y máquina",
				30,
				25000,
				null);

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.save(any(Servicio.class))).thenAnswer(invocacion -> {
			Servicio guardado = invocacion.getArgument(0);
			guardado.setId(10L);
			return guardado;
		});

		RespuestaServicio respuesta = servicioServicio.crear(1L, solicitud);

		ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);
		verify(servicioRepositorio).save(captor.capture());

		Servicio entidad = captor.getValue();
		assertEquals(salon, entidad.getSalon());
		assertTrue(entidad.isActivo());

		assertEquals(10L, respuesta.id());
		assertEquals("Corte clásico", respuesta.nombre());
		assertEquals("Corte con tijera y máquina", respuesta.descripcion());
		assertEquals(30, respuesta.duracionMinutos());
		assertEquals(25000, respuesta.precio());
		assertTrue(respuesta.activo());
	}

	@Test
	void crearRespetaActivoFalseCuandoSeEnvia() {
		SolicitudServicio solicitud = new SolicitudServicio(
				"Tratamiento especial",
				null,
				60,
				50000,
				false);

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.save(any(Servicio.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		RespuestaServicio respuesta = servicioServicio.crear(1L, solicitud);

		assertFalse(respuesta.activo());
	}

	@Test
	void listarFiltraExclusivamentePorSalonAutenticado() {
		Servicio servicio1 = crearServicio(1L, "Corte clásico", 30, 25000, true);
		Servicio servicio2 = crearServicio(2L, "Barba completa", 20, 18000, true);

		when(servicioRepositorio.findAllBySalonIdOrderByIdAsc(1L))
				.thenReturn(List.of(servicio1, servicio2));

		List<RespuestaServicio> resultado = servicioServicio.listar(1L);

		assertEquals(2, resultado.size());
		assertEquals("Corte clásico", resultado.get(0).nombre());
		assertEquals("Barba completa", resultado.get(1).nombre());
		verify(servicioRepositorio).findAllBySalonIdOrderByIdAsc(1L);
		verifyNoMoreInteractions(servicioRepositorio);
	}

	@Test
	void actualizarModificaServicioDelMismoSalon() {
		Servicio existente = crearServicio(5L, "Corte básico", 30, 20000, true);
		SolicitudServicio solicitud = new SolicitudServicio(
				"Corte premium",
				"Incluye lavado",
				45,
				35000,
				false);

		when(servicioRepositorio.findByIdAndSalonId(5L, 1L))
				.thenReturn(Optional.of(existente));
		when(servicioRepositorio.save(existente)).thenReturn(existente);

		RespuestaServicio respuesta = servicioServicio.actualizar(1L, 5L, solicitud);

		assertEquals("Corte premium", respuesta.nombre());
		assertEquals("Incluye lavado", respuesta.descripcion());
		assertEquals(45, respuesta.duracionMinutos());
		assertEquals(35000, respuesta.precio());
		assertFalse(respuesta.activo());
		verify(servicioRepositorio).save(existente);
	}

	@Test
	void actualizarRechazaServicioDeOtroSalonOInexistenteCon404() {
		SolicitudServicio solicitud = new SolicitudServicio(
				"Corte premium",
				"Incluye lavado",
				45,
				35000,
				true);

		when(servicioRepositorio.findByIdAndSalonId(99L, 1L))
				.thenReturn(Optional.empty());

		assertThrows(
				ExcepcionRecursoNoEncontrado.class,
				() -> servicioServicio.actualizar(1L, 99L, solicitud));

		verify(servicioRepositorio, never()).save(any(Servicio.class));
	}

	private Servicio crearServicio(Long id, String nombre, int duracion, int precio, boolean activo) {
		Servicio servicio = new Servicio();
		servicio.setId(id);
		servicio.setNombre(nombre);
		servicio.setDuracionMinutos(duracion);
		servicio.setPrecio(precio);
		servicio.setActivo(activo);
		servicio.setSalon(salon);
		return servicio;
	}

}

package com.devSenior.sillon.chat.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.chat.entidad.EstadoSolicitud;
import com.devSenior.sillon.chat.entidad.MotivoSolicitud;
import com.devSenior.sillon.chat.entidad.SolicitudContacto;
import com.devSenior.sillon.chat.repositorio.SolicitudContactoRepositorio;
import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class SolicitudContactoServicioTest {

	@Mock
	private SolicitudContactoRepositorio solicitudRepositorio;
	@Mock
	private SalonRepositorio salonRepositorio;

	private SolicitudContactoServicio servicio;

	@BeforeEach
	void preparar() {
		Clock reloj = Clock.fixed(LocalDateTime.of(2026, 12, 1, 10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
				ZoneId.of("America/Bogota"));
		servicio = new SolicitudContactoServicio(solicitudRepositorio, salonRepositorio, reloj);
	}

	@Test
	void listaRecientesAtiendeYRechazaAjenaORepetida() {
		SolicitudContacto reciente = solicitud(2L, EstadoSolicitud.PENDIENTE, LocalDateTime.of(2026, 12, 2, 9, 0));
		SolicitudContacto antigua = solicitud(1L, EstadoSolicitud.ATENDIDA, LocalDateTime.of(2026, 12, 1, 9, 0));
		when(solicitudRepositorio.findBySalon_IdOrderByFechaCreacionDescIdDesc(1L))
				.thenReturn(List.of(reciente, antigua));
		assertEquals(List.of(2L, 1L), servicio.listar(1L, null).stream().map(item -> item.id()).toList());

		when(solicitudRepositorio.findBySalon_IdAndEstadoOrderByFechaCreacionDescIdDesc(1L, EstadoSolicitud.PENDIENTE))
				.thenReturn(List.of(reciente));
		assertEquals(1, servicio.listar(1L, EstadoSolicitud.PENDIENTE).size());

		when(solicitudRepositorio.findByIdAndSalon_Id(2L, 1L)).thenReturn(Optional.of(reciente));
		when(solicitudRepositorio.saveAndFlush(reciente)).thenReturn(reciente);
		assertEquals(EstadoSolicitud.ATENDIDA, servicio.atender(1L, 2L).estado());
		assertThrows(ConflictoException.class, () -> servicio.atender(1L, 2L));

		when(solicitudRepositorio.findByIdAndSalon_Id(9L, 1L)).thenReturn(Optional.empty());
		assertThrows(NoEncontradoException.class, () -> servicio.atender(1L, 9L));
	}

	@Test
	void creaPendientePorIa() {
		Salon salon = new Salon();
		salon.setId(1L);
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(solicitudRepositorio.save(any(SolicitudContacto.class))).thenAnswer(invocacion -> {
			SolicitudContacto solicitud = invocacion.getArgument(0);
			solicitud.setId(4L);
			assertEquals(MotivoSolicitud.PEDIDO_POR_IA, solicitud.getMotivo());
			assertEquals(EstadoSolicitud.PENDIENTE, solicitud.getEstado());
			assertEquals(2000, solicitud.getMensajes().length());
			return solicitud;
		});
		assertEquals(4L, servicio.crear(1L, "Ana", "300", "x".repeat(2500), MotivoSolicitud.PEDIDO_POR_IA));
	}

	private SolicitudContacto solicitud(Long id, EstadoSolicitud estado, LocalDateTime fecha) {
		SolicitudContacto solicitud = new SolicitudContacto();
		solicitud.setId(id);
		solicitud.setClienteNombre("Ana");
		solicitud.setClienteContacto("300");
		solicitud.setMensajes("hola");
		solicitud.setMotivo(MotivoSolicitud.PEDIDO_POR_IA);
		solicitud.setEstado(estado);
		solicitud.setFechaCreacion(fecha);
		return solicitud;
	}

}

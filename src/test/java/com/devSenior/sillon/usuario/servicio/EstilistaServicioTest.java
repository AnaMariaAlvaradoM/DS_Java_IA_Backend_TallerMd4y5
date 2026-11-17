package com.devSenior.sillon.usuario.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;
import com.devSenior.sillon.servicio.entidad.Servicio;
import com.devSenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devSenior.sillon.usuario.dto.EstilistaRespuesta;
import com.devSenior.sillon.usuario.dto.SolicitudEstilista;
import com.devSenior.sillon.usuario.entidad.Rol;
import com.devSenior.sillon.usuario.entidad.Usuario;
import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@ExtendWith(MockitoExtension.class)
class EstilistaServicioTest {

	@Mock
	private UsuarioRepositorio usuarioRepositorio;

	@Mock
	private SalonRepositorio salonRepositorio;

	@Mock
	private ServicioRepositorio servicioRepositorio;

	private EstilistaServicio estilistaServicio;

	@BeforeEach
	void preparar() {
		estilistaServicio = new EstilistaServicio(usuarioRepositorio, salonRepositorio, servicioRepositorio,
				new BCryptPasswordEncoder(), 2);
	}

	@Test
	void elTercerEstilistaGratisResponde402AntesDeCorreoYServicio() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(Plan.GRATIS)));
		when(usuarioRepositorio.countBySalon_IdAndRol(1L, Rol.ESTILISTA)).thenReturn(2L);

		assertThrows(PagoRequeridoException.class, () -> estilistaServicio.crear(1L, solicitud("mateo@sillon.test", 9L)));
		verify(usuarioRepositorio, never()).existsByEmail(any());
		verify(servicioRepositorio, never()).findByIdAndSalon_Id(any(), any());
	}

	@Test
	void enProElTercerEstilistaSiSeCrea() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(Plan.PRO)));
		when(usuarioRepositorio.existsByEmail("sara@sillon.test")).thenReturn(false);
		when(servicioRepositorio.findByIdAndSalon_Id(3L, 1L)).thenReturn(Optional.of(servicio(3L, "Corte", true)));
		when(usuarioRepositorio.save(any(Usuario.class))).thenAnswer(invocacion -> {
			Usuario usuario = invocacion.getArgument(0);
			usuario.setId(8L);
			return usuario;
		});

		EstilistaRespuesta respuesta = estilistaServicio.crear(1L, solicitud("sara@sillon.test", 3L));

		assertEquals("sara@sillon.test", respuesta.email());
		assertEquals(List.of("Corte"), respuesta.servicios());
		assertTrue(respuesta.activo());
	}

	@Test
	void elCorreoRepetidoResponde409AntesDelServicio() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(Plan.GRATIS)));
		when(usuarioRepositorio.countBySalon_IdAndRol(1L, Rol.ESTILISTA)).thenReturn(1L);
		when(usuarioRepositorio.existsByEmail("mateo@sillon.test")).thenReturn(true);

		assertThrows(ConflictoException.class, () -> estilistaServicio.crear(1L, solicitud("mateo@sillon.test", 99L)));
		verify(servicioRepositorio, never()).findByIdAndSalon_Id(any(), any());
	}

	@Test
	void unServicioAjenoResponde404() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(Plan.GRATIS)));
		when(usuarioRepositorio.countBySalon_IdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("nuevo@sillon.test")).thenReturn(false);
		when(servicioRepositorio.findByIdAndSalon_Id(50L, 1L)).thenReturn(Optional.empty());

		assertThrows(NoEncontradoException.class, () -> estilistaServicio.crear(1L, solicitud("nuevo@sillon.test", 50L)));
	}

	@Test
	void unServicioInactivoResponde400() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon(Plan.PRO)));
		when(usuarioRepositorio.existsByEmail("nuevo@sillon.test")).thenReturn(false);
		when(servicioRepositorio.findByIdAndSalon_Id(3L, 1L)).thenReturn(Optional.of(servicio(3L, "Corte", false)));

		assertThrows(FirmaInvalidaException.class, () -> estilistaServicio.crear(1L, solicitud("nuevo@sillon.test", 3L)));
	}

	@Test
	void asignarReemplazaYRechazaInactivoOAjeno() {
		Usuario estilista = new Usuario();
		estilista.setId(8L);
		estilista.setNombre("Mateo");
		estilista.setEmail("mateo@sillon.test");
		estilista.setRol(Rol.ESTILISTA);
		estilista.setActivo(true);
		when(usuarioRepositorio.findByIdAndSalon_Id(8L, 1L)).thenReturn(Optional.of(estilista));
		when(servicioRepositorio.findByIdAndSalon_Id(3L, 1L)).thenReturn(Optional.of(servicio(3L, "Corte", true)));
		when(servicioRepositorio.findByIdAndSalon_Id(4L, 1L)).thenReturn(Optional.of(servicio(4L, "Barba", true)));
		when(usuarioRepositorio.save(estilista)).thenReturn(estilista);

		var respuesta = estilistaServicio.asignarServicios(1L, 8L, List.of(4L, 3L));
		assertEquals(List.of("Corte", "Barba"), respuesta.servicios());
		assertTrue(estilista.ofreceServicio(3L));

		when(servicioRepositorio.findByIdAndSalon_Id(5L, 1L)).thenReturn(Optional.of(servicio(5L, "Tinte", false)));
		assertThrows(FirmaInvalidaException.class, () -> estilistaServicio.asignarServicios(1L, 8L, List.of(5L)));

		when(usuarioRepositorio.findByIdAndSalon_Id(8L, 2L)).thenReturn(Optional.empty());
		assertThrows(NoEncontradoException.class, () -> estilistaServicio.asignarServicios(2L, 8L, List.of(3L)));
	}

	private SolicitudEstilista solicitud(String email, Long servicioId) {
		return new SolicitudEstilista("Mateo", email, "clave1234", List.of(servicioId));
	}

	private Salon salon(Plan plan) {
		Salon salon = new Salon();
		salon.setId(1L);
		salon.setPlan(plan);
		return salon;
	}

	private Servicio servicio(Long id, String nombre, boolean activo) {
		Servicio servicio = new Servicio();
		servicio.setId(id);
		servicio.setNombre(nombre);
		servicio.setActivo(activo);
		return servicio;
	}

}

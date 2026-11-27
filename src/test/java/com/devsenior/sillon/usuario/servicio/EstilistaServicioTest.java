package com.devsenior.sillon.usuario.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.dto.RespuestaEstilista;
import com.devsenior.sillon.usuario.dto.SolicitudEstilista;
import com.devsenior.sillon.usuario.mapper.EstilistaMapper;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

class EstilistaServicioTest {

	private SalonRepositorio salonRepositorio;
	private UsuarioRepositorio usuarioRepositorio;
	private ServicioRepositorio servicioRepositorio;
	private BCryptPasswordEncoder passwordEncoder;
	private EstilistaServicio estilistaServicio;
	private Salon salon;
	private Servicio corteClasico;
	private Servicio barbaCompleta;

	@BeforeEach
	void preparar() {
		salonRepositorio = mock(SalonRepositorio.class);
		usuarioRepositorio = mock(UsuarioRepositorio.class);
		servicioRepositorio = mock(ServicioRepositorio.class);
		passwordEncoder = new BCryptPasswordEncoder();

		estilistaServicio = new EstilistaServicio(
				salonRepositorio,
				usuarioRepositorio,
				servicioRepositorio,
				passwordEncoder,
				new EstilistaMapper(),
				2);

		salon = new Salon();
		salon.setId(1L);
		salon.setNombre("Barbería La Navaja");
		salon.setPlan(Plan.GRATIS);

		corteClasico = crearServicio(10L, "Corte clásico", true);
		barbaCompleta = crearServicio(11L, "Barba completa", true);
	}

	@Test
	void registrarMateoConDosServiciosCreaEstilistaCorrectamente() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"  MATEO@salon.example  ",
				"ClaveMateo2026",
				List.of(10L, 11L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L, 11L), 1L))
				.thenReturn(List.of(corteClasico, barbaCompleta));
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
			Usuario guardado = invocacion.getArgument(0);
			guardado.setId(101L);
			return guardado;
		});

		RespuestaEstilista respuesta = estilistaServicio.registrar(1L, solicitud);

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepositorio).saveAndFlush(captor.capture());

		Usuario guardado = captor.getValue();
		assertEquals("Mateo", guardado.getNombre());
		assertEquals("mateo@salon.example", guardado.getEmail());
		assertEquals(Rol.ESTILISTA, guardado.getRol());
		assertTrue(guardado.isActivo());
		assertEquals(salon, guardado.getSalon());
		assertTrue(passwordEncoder.matches("ClaveMateo2026", guardado.getPassword()));

		assertEquals(101L, respuesta.id());
		assertEquals("Mateo", respuesta.nombre());
		assertEquals("mateo@salon.example", respuesta.email());
		assertTrue(respuesta.activo());
		assertEquals(List.of("Corte clásico", "Barba completa"), respuesta.servicios());
	}

	@Test
	void registrarSaraConUnServicioComoSegundoEstilistaEnPlanGratis() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Sara",
				"sara@salon.example",
				"ClaveSara2026",
				List.of(10L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(1L);
		when(usuarioRepositorio.existsByEmail("sara@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L), 1L))
				.thenReturn(List.of(corteClasico));
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class))).thenAnswer(invocacion -> {
			Usuario guardado = invocacion.getArgument(0);
			guardado.setId(102L);
			return guardado;
		});

		RespuestaEstilista respuesta = estilistaServicio.registrar(1L, solicitud);

		assertEquals(102L, respuesta.id());
		assertEquals("Sara", respuesta.nombre());
		assertEquals("sara@salon.example", respuesta.email());
		assertEquals(List.of("Corte clásico"), respuesta.servicios());
	}

	@Test
	void registrarPermiteAsignarServicioInactivoDelMismoSalon() {
		Servicio servicioInactivo = crearServicio(12L, "Tintura temporal", false);
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Sara",
				"sara@salon.example",
				"ClaveSara2026",
				List.of(12L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("sara@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(12L), 1L))
				.thenReturn(List.of(servicioInactivo));
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		RespuestaEstilista respuesta = estilistaServicio.registrar(1L, solicitud);

		assertEquals(List.of("Tintura temporal"), respuesta.servicios());
	}

	@Test
	void registrarTercerEstilistaEnPlanGratisLanza402AntesDeValidarCorreoOServicios() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Tercero",
				"duplicado@salon.example",
				"ClaveTercero2026",
				List.of(999L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(2L);

		assertThrows(
				ExcepcionPagoRequerido.class,
				() -> estilistaServicio.registrar(1L, solicitud));

		verify(usuarioRepositorio, never()).existsByEmail(any());
		verify(usuarioRepositorio, never()).saveAndFlush(any());
		verifyNoInteractions(servicioRepositorio);
	}

	@Test
	void registrarPermiteTercerEstilistaEnPlanPro() {
		salon.setPlan(Plan.PRO);
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Lucía",
				"lucia@salon.example",
				"ClaveLucia2026",
				List.of(10L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.existsByEmail("lucia@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L), 1L))
				.thenReturn(List.of(corteClasico));
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		RespuestaEstilista respuesta = estilistaServicio.registrar(1L, solicitud);

		assertEquals("Lucía", respuesta.nombre());
		verify(usuarioRepositorio, never()).countBySalonIdAndRol(any(), any());
	}

	@Test
	void registrarRechazaCorreoDuplicadoCon409AntesDeValidarServicios() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"  Mateo@Salon.Example ",
				"ClaveMateo2026",
				List.of(999L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(true);

		assertThrows(
				ExcepcionConflicto.class,
				() -> estilistaServicio.registrar(1L, solicitud));

		verifyNoInteractions(servicioRepositorio);
		verify(usuarioRepositorio, never()).saveAndFlush(any());
	}

	@Test
	void registrarConvierteColisionConcurrenteDeCorreoEn409() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"mateo@salon.example",
				"ClaveMateo2026",
				List.of(10L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L), 1L))
				.thenReturn(List.of(corteClasico));

		SQLException sqlDuplicado = new SQLException(
				"Duplicate entry 'mateo@salon.example' for key 'usuarios.UK_email'",
				"23000",
				1062);
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class)))
				.thenThrow(new DataIntegrityViolationException("error de unicidad", sqlDuplicado));

		assertThrows(
				ExcepcionConflicto.class,
				() -> estilistaServicio.registrar(1L, solicitud));
	}

	@Test
	void registrarNoConvierteOtraViolacionDeIntegridadEn409() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"mateo@salon.example",
				"ClaveMateo2026",
				List.of(10L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L), 1L))
				.thenReturn(List.of(corteClasico));

		SQLException sqlOtro = new SQLException(
				"Column 'nombre' cannot be null",
				"23000",
				1048);
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class)))
				.thenThrow(new DataIntegrityViolationException("integridad", sqlOtro));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> estilistaServicio.registrar(1L, solicitud));
	}

	@Test
	void registrarRechazaServicioDeOtroSalonOInexistenteCon404() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"mateo@salon.example",
				"ClaveMateo2026",
				List.of(10L, 999L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L, 999L), 1L))
				.thenReturn(List.of(corteClasico));

		assertThrows(
				ExcepcionRecursoNoEncontrado.class,
				() -> estilistaServicio.registrar(1L, solicitud));

		verify(usuarioRepositorio, never()).saveAndFlush(any());
	}

	@Test
	void registrarDeduplicaServiciosRepetidosDelMismoSalon() {
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"mateo@salon.example",
				"ClaveMateo2026",
				List.of(10L, 10L));

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(usuarioRepositorio.countBySalonIdAndRol(1L, Rol.ESTILISTA)).thenReturn(0L);
		when(usuarioRepositorio.existsByEmail("mateo@salon.example")).thenReturn(false);
		when(servicioRepositorio.findAllByIdInAndSalonId(List.of(10L), 1L))
				.thenReturn(List.of(corteClasico));
		when(usuarioRepositorio.saveAndFlush(any(Usuario.class)))
				.thenAnswer(invocacion -> invocacion.getArgument(0));

		RespuestaEstilista respuesta = estilistaServicio.registrar(1L, solicitud);

		assertEquals(List.of("Corte clásico"), respuesta.servicios());
	}

	@Test
	void registrarRechazaPasswordQueExcede72BytesUtf8() {
		String passwordConTildes = "á".repeat(40);
		SolicitudEstilista solicitud = new SolicitudEstilista(
				"Mateo",
				"mateo@salon.example",
				passwordConTildes,
				List.of(10L));

		assertThrows(
				ExcepcionSolicitudInvalida.class,
				() -> estilistaServicio.registrar(1L, solicitud));

		verifyNoInteractions(salonRepositorio, usuarioRepositorio, servicioRepositorio);
	}

	@Test
	void listarDevuelveSoloEstilistasDelSalonConSusServicios() {
		Usuario mateo = new Usuario();
		mateo.setId(101L);
		mateo.setNombre("Mateo");
		mateo.setEmail("mateo@salon.example");
		mateo.setRol(Rol.ESTILISTA);
		mateo.setActivo(true);
		mateo.setSalon(salon);
		mateo.setServicios(List.of(corteClasico, barbaCompleta));

		when(usuarioRepositorio.buscarEstilistasConServicios(1L, Rol.ESTILISTA))
				.thenReturn(List.of(mateo));

		List<RespuestaEstilista> respuesta = estilistaServicio.listar(1L);

		assertEquals(1, respuesta.size());
		assertEquals("Mateo", respuesta.get(0).nombre());
		assertEquals(List.of("Corte clásico", "Barba completa"), respuesta.get(0).servicios());
		verify(usuarioRepositorio).buscarEstilistasConServicios(1L, Rol.ESTILISTA);
	}

	private Servicio crearServicio(Long id, String nombre, boolean activo) {
		Servicio servicio = new Servicio();
		servicio.setId(id);
		servicio.setNombre(nombre);
		servicio.setDuracionMinutos(30);
		servicio.setPrecio(25000);
		servicio.setActivo(activo);
		servicio.setSalon(salon);
		return servicio;
	}

}

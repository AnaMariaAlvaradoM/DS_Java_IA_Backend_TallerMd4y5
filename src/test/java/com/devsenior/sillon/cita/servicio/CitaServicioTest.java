package com.devsenior.sillon.cita.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCambiarEstadoCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.mapper.CitaMapper;
import com.devsenior.sillon.cita.modelo.Cita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.modelo.OrigenCita;
import com.devsenior.sillon.cita.repositorio.CitaRepositorio;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.cliente.repositorio.ClienteRepositorio;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoPermitida;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.salon.modelo.ConfiguracionSalon;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.servicio.repositorio.ServicioRepositorio;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.repositorio.UsuarioRepositorio;

class CitaServicioTest {

	private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
	// Miércoles 2026-11-25 10:00:00 en America/Bogota
	private static final Instant AHORA = Instant.parse("2026-11-25T15:00:00Z");

	private SalonRepositorio salonRepositorio;
	private ServicioRepositorio servicioRepositorio;
	private UsuarioRepositorio usuarioRepositorio;
	private ClienteRepositorio clienteRepositorio;
	private CitaRepositorio citaRepositorio;
	private ReglasAgenda reglasAgenda;
	private Clock reloj;
	private CitaServicio citaServicio;

	private Salon salon;
	private Servicio servicio;
	private Usuario estilista;

	@BeforeEach
	void preparar() {
		salonRepositorio = mock(SalonRepositorio.class);
		servicioRepositorio = mock(ServicioRepositorio.class);
		usuarioRepositorio = mock(UsuarioRepositorio.class);
		clienteRepositorio = mock(ClienteRepositorio.class);
		citaRepositorio = mock(CitaRepositorio.class);
		reloj = Clock.fixed(AHORA, ZONA_BOGOTA);
		reglasAgenda = new ReglasAgenda(reloj);

		citaServicio = new CitaServicio(
				salonRepositorio,
				servicioRepositorio,
				usuarioRepositorio,
				clienteRepositorio,
				citaRepositorio,
				reglasAgenda,
				new CitaMapper(),
				reloj);

		salon = new Salon();
		salon.setId(1L);
		salon.setNombre("Barbería La Navaja");
		salon.setPlan(Plan.GRATIS);
		salon.setConfiguracion(ConfiguracionSalon.predeterminada());

		servicio = new Servicio();
		servicio.setId(10L);
		servicio.setNombre("Corte clásico");
		servicio.setDuracionMinutos(30);
		servicio.setPrecio(25000);
		servicio.setActivo(true);
		servicio.setSalon(salon);

		estilista = new Usuario();
		estilista.setId(20L);
		estilista.setNombre("Mateo");
		estilista.setEmail("mateo@salon.example");
		estilista.setRol(Rol.ESTILISTA);
		estilista.setActivo(true);
		estilista.setSalon(salon);
		estilista.setServicios(List.of(servicio));
	}

	@Test
	void crearCitaValidaCreaClienteNormalizadoCopiaPrecioYGuardaConfirmadaYSalon() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);
		SolicitudCrearCita solicitud = new SolicitudCrearCita(
				"  Carlos Pérez  ",
				" +57 300-111-2233 ",
				10L,
				20L,
				inicio,
				"Corte con degradado");

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), eq(inicio), eq(inicio.plusMinutes(30))))
				.thenReturn(List.of());
		when(clienteRepositorio.findBySalonIdAndTelefono(1L, "+573001112233"))
				.thenReturn(Optional.empty());
		when(clienteRepositorio.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> {
			Cliente c = inv.getArgument(0);
			c.setId(30L);
			return c;
		});
		when(citaRepositorio.saveAndFlush(any(Cita.class))).thenAnswer(inv -> {
			Cita c = inv.getArgument(0);
			c.setId(100L);
			LocalDateTime ahora = LocalDateTime.now(reloj);
			c.setFechaCreacion(ahora);
			c.setFechaActualizacion(ahora);
			return c;
		});

		RespuestaCita respuesta = citaServicio.crear(1L, solicitud);

		ArgumentCaptor<Cliente> clienteCaptor = ArgumentCaptor.forClass(Cliente.class);
		verify(clienteRepositorio).saveAndFlush(clienteCaptor.capture());
		assertEquals("Carlos Pérez", clienteCaptor.getValue().getNombre());
		assertEquals("+573001112233", clienteCaptor.getValue().getTelefono());

		assertEquals(100L, respuesta.id());
		assertEquals(inicio, respuesta.inicio());
		assertEquals(inicio.plusMinutes(30), respuesta.fin());
		assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
		assertEquals(OrigenCita.SALON, respuesta.origen());
		assertEquals(25000, respuesta.precio());
		assertEquals(10L, respuesta.servicioId());
		assertEquals("Corte clásico", respuesta.servicioNombre());
		assertEquals(20L, respuesta.estilistaId());
		assertEquals("Mateo", respuesta.estilistaNombre());
		assertEquals("Carlos Pérez", respuesta.clienteNombre());
		assertEquals("+573001112233", respuesta.clienteTelefono());
		assertEquals("Corte con degradado", respuesta.nota());
	}

	@Test
	void crearCitaReutilizaClienteExistenteEnElSalonYConservaPrecioDelMomento() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 11, 0);
		Cliente existente = new Cliente();
		existente.setId(31L);
		existente.setNombre("Laura Gómez");
		existente.setTelefono("3001112233");
		existente.setSalon(salon);

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), eq(inicio), eq(inicio.plusMinutes(30))))
				.thenReturn(List.of());
		when(clienteRepositorio.findBySalonIdAndTelefono(1L, "3001112233"))
				.thenReturn(Optional.of(existente));
		when(citaRepositorio.saveAndFlush(any(Cita.class))).thenAnswer(inv -> {
			Cita c = inv.getArgument(0);
			c.setId(101L);
			return c;
		});

		SolicitudCrearCita solicitud = new SolicitudCrearCita(
				"Otro Nombre",
				"300 111 2233",
				10L,
				20L,
				inicio,
				null);

		RespuestaCita respuesta = citaServicio.crear(1L, solicitud);

		verify(clienteRepositorio, never()).saveAndFlush(any());
		assertEquals("Laura Gómez", respuesta.clienteNombre());
		assertEquals(25000, respuesta.precio());

		servicio.setPrecio(40000);
		assertEquals(25000, respuesta.precio());
	}

	@Test
	void crearRechazaServicioInexistenteODeOtroSalonCon404AntesDeConsultarEstilista() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(999L, 1L)).thenReturn(Optional.empty());

		SolicitudCrearCita solicitud = solicitudEn(LocalDateTime.of(2026, 11, 26, 10, 0), 999L, 20L);

		assertThrows(ExcepcionRecursoNoEncontrado.class, () -> citaServicio.crear(1L, solicitud));

		verifyNoInteractions(usuarioRepositorio, citaRepositorio, clienteRepositorio);
	}

	@Test
	void crearRechazaServicioInactivoCon409AntesDeConsultarEstilista() {
		servicio.setActivo(false);
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));

		SolicitudCrearCita solicitud = solicitudEn(LocalDateTime.of(2026, 11, 26, 10, 0), 10L, 20L);

		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, solicitud));

		verifyNoInteractions(usuarioRepositorio, citaRepositorio, clienteRepositorio);
	}

	@Test
	void crearRechazaEstilistaInexistenteODeOtroSalonCon404() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(999L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.empty());

		SolicitudCrearCita solicitud = solicitudEn(LocalDateTime.of(2026, 11, 26, 10, 0), 10L, 999L);

		assertThrows(ExcepcionRecursoNoEncontrado.class, () -> citaServicio.crear(1L, solicitud));

		verifyNoInteractions(citaRepositorio, clienteRepositorio);
	}

	@Test
	void crearRechazaEstilistaInactivoOQueNoOfreceElServicioCon409() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));

		estilista.setActivo(false);
		SolicitudCrearCita solicitud = solicitudEn(LocalDateTime.of(2026, 11, 24, 10, 0), 10L, 20L);
		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, solicitud));

		estilista.setActivo(true);
		estilista.setServicios(List.of());
		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, solicitud));

		verifyNoInteractions(citaRepositorio, clienteRepositorio);
	}

	@Test
	void crearRechazaCitaEnElPasadoYFueraDeHorarioAntesDeConsultarCruces() {
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));

		SolicitudCrearCita enPasado = solicitudEn(LocalDateTime.of(2026, 11, 25, 9, 0), 10L, 20L);
		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, enPasado));

		SolicitudCrearCita domingo = solicitudEn(LocalDateTime.of(2026, 11, 29, 10, 0), 10L, 20L);
		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, domingo));

		SolicitudCrearCita excedeCierre = solicitudEn(LocalDateTime.of(2026, 11, 26, 18, 45), 10L, 20L);
		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, excedeCierre));

		verifyNoInteractions(citaRepositorio, clienteRepositorio);
	}

	@Test
	void crearRechazaCruceConCitaActivaYPermiteIntervaloContiguo() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);
		Cita existente = new Cita();
		existente.setInicio(LocalDateTime.of(2026, 11, 26, 10, 15));
		existente.setFin(LocalDateTime.of(2026, 11, 26, 10, 45));
		existente.setEstado(EstadoCita.PENDIENTE);

		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), eq(inicio), eq(inicio.plusMinutes(30))))
				.thenReturn(List.of(existente));

		assertThrows(ExcepcionConflicto.class, () -> citaServicio.crear(1L, solicitudEn(inicio, 10L, 20L)));
		verifyNoInteractions(clienteRepositorio);

		// Intervalo contiguo 10:45 - 11:15 permitido
		LocalDateTime inicioContiguo = LocalDateTime.of(2026, 11, 26, 10, 45);
		Cliente cliente = new Cliente();
		cliente.setId(30L);
		cliente.setNombre("Carlos");
		cliente.setTelefono("3001112233");

		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), eq(inicioContiguo), eq(inicioContiguo.plusMinutes(30))))
				.thenReturn(List.of(existente));
		when(clienteRepositorio.findBySalonIdAndTelefono(1L, "3001112233"))
				.thenReturn(Optional.of(cliente));
		when(citaRepositorio.saveAndFlush(any(Cita.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		RespuestaCita respuesta = citaServicio.crear(1L, solicitudEn(inicioContiguo, 10L, 20L));
		assertEquals(inicioContiguo, respuesta.inicio());
	}

	@Test
	void crearRechazaTelefonoSinDigitosSuficientesCon400() {
		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L)).thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), eq(inicio), eq(inicio.plusMinutes(30))))
				.thenReturn(List.of());

		SolicitudCrearCita solicitud = new SolicitudCrearCita(
				"Carlos",
				"---+++---",
				10L,
				20L,
				inicio,
				null);

		assertThrows(ExcepcionSolicitudInvalida.class, () -> citaServicio.crear(1L, solicitud));
		verifyNoInteractions(clienteRepositorio);
	}

	@Test
	void listarUsaFechaActualDeColombiaPorDefectoYFiltraPorRol() {
		LocalDate hoyBogota = LocalDate.of(2026, 11, 25);
		Cita cita = crearCitaPersistida(100L, EstadoCita.CONFIRMADA);

		when(citaRepositorio.buscarPorSalonYRango(
				1L, hoyBogota.atStartOfDay(), hoyBogota.plusDays(1).atStartOfDay()))
				.thenReturn(List.of(cita));

		List<RespuestaCita> deDueno = citaServicio.listarPorFecha(1L, 5L, Rol.DUENO, null);
		assertEquals(1, deDueno.size());
		verify(citaRepositorio).buscarPorSalonYRango(
				1L, hoyBogota.atStartOfDay(), hoyBogota.plusDays(1).atStartOfDay());

		LocalDate otraFecha = LocalDate.of(2026, 11, 26);
		when(citaRepositorio.buscarPorSalonEstilistaYRango(
				1L, 20L, otraFecha.atStartOfDay(), otraFecha.plusDays(1).atStartOfDay()))
				.thenReturn(List.of(cita));

		List<RespuestaCita> deEstilista = citaServicio.listarPorFecha(1L, 20L, Rol.ESTILISTA, otraFecha);
		assertEquals(1, deEstilista.size());
		verify(citaRepositorio).buscarPorSalonEstilistaYRango(
				1L, 20L, otraFecha.atStartOfDay(), otraFecha.plusDays(1).atStartOfDay());
	}

	@Test
	void cambiarEstadoPorDuenoUsaSaveAndFlushYValidaTransiciones() {
		Cita cita = crearCitaPersistida(100L, EstadoCita.CONFIRMADA);
		when(citaRepositorio.buscarPorIdYSalon(100L, 1L)).thenReturn(Optional.of(cita));
		when(citaRepositorio.saveAndFlush(cita)).thenAnswer(inv -> {
			Cita c = inv.getArgument(0);
			c.setFechaActualizacion(LocalDateTime.of(2026, 11, 25, 10, 5, 0, 500_000_000));
			return c;
		});

		RespuestaCita respuesta = citaServicio.cambiarEstado(
				1L, 5L, Rol.DUENO, 100L, new SolicitudCambiarEstadoCita(EstadoCita.CANCELADA));

		assertEquals(EstadoCita.CANCELADA, respuesta.estado());
		assertEquals(LocalDateTime.of(2026, 11, 25, 10, 5, 0, 500_000_000), respuesta.fechaActualizacion());
		verify(citaRepositorio).saveAndFlush(cita);
	}

	@Test
	void cambiarEstadoRechazaTransicionInvalidaCon409YCitaDeOtroSalonCon404() {
		Cita completada = crearCitaPersistida(100L, EstadoCita.COMPLETADA);
		when(citaRepositorio.buscarPorIdYSalon(100L, 1L)).thenReturn(Optional.of(completada));
		when(citaRepositorio.buscarPorIdYSalon(999L, 1L)).thenReturn(Optional.empty());

		assertThrows(
				ExcepcionConflicto.class,
				() -> citaServicio.cambiarEstado(
						1L, 5L, Rol.DUENO, 100L, new SolicitudCambiarEstadoCita(EstadoCita.CANCELADA)));

		assertThrows(
				ExcepcionRecursoNoEncontrado.class,
				() -> citaServicio.cambiarEstado(
						1L, 5L, Rol.DUENO, 999L, new SolicitudCambiarEstadoCita(EstadoCita.CANCELADA)));

		verify(citaRepositorio, never()).saveAndFlush(any());
	}

	@Test
	void cambiarEstadoPorEstilistaRespetaPermisos404_403_y_409() {
		Cita confirmadaPropia = crearCitaPersistida(100L, EstadoCita.CONFIRMADA);
		when(citaRepositorio.buscarPorIdYSalon(100L, 1L)).thenReturn(Optional.of(confirmadaPropia));
		when(citaRepositorio.saveAndFlush(confirmadaPropia)).thenReturn(confirmadaPropia);

		// Cita de otro estilista -> 404
		assertThrows(
				ExcepcionRecursoNoEncontrado.class,
				() -> citaServicio.cambiarEstado(
						1L, 99L, Rol.ESTILISTA, 100L, new SolicitudCambiarEstadoCita(EstadoCita.COMPLETADA)));

		// Estado destino distinto de COMPLETADA / NO_ASISTIO -> 403
		assertThrows(
				ExcepcionNoPermitida.class,
				() -> citaServicio.cambiarEstado(
						1L, 20L, Rol.ESTILISTA, 100L, new SolicitudCambiarEstadoCita(EstadoCita.CANCELADA)));

		// Cita PENDIENTE a COMPLETADA (destino autorizado pero transición inválida) ->
		// 409
		Cita pendientePropia = crearCitaPersistida(101L, EstadoCita.PENDIENTE);
		when(citaRepositorio.buscarPorIdYSalon(101L, 1L)).thenReturn(Optional.of(pendientePropia));
		assertThrows(
				ExcepcionConflicto.class,
				() -> citaServicio.cambiarEstado(
						1L, 20L, Rol.ESTILISTA, 101L, new SolicitudCambiarEstadoCita(EstadoCita.COMPLETADA)));

		// Estilista cambia su cita CONFIRMADA a NO_ASISTIO -> 200
		RespuestaCita respuesta = citaServicio.cambiarEstado(
				1L, 20L, Rol.ESTILISTA, 100L, new SolicitudCambiarEstadoCita(EstadoCita.NO_ASISTIO));
		assertEquals(EstadoCita.NO_ASISTIO, respuesta.estado());
		verify(citaRepositorio).saveAndFlush(confirmadaPropia);
	}

	@Test
	void reservaPublicaCreaCitaPendienteEnLineaConPrecioYDuracion() {
		salon.setSlug("barberia-pruebas");

		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);
		SolicitudCrearCita solicitud = solicitudEn(inicio, 10L, 20L);

		when(salonRepositorio.findBySlug("barberia-pruebas"))
				.thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L))
				.thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(
				20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(),
				eq(inicio), eq(inicio.plusMinutes(30))))
				.thenReturn(List.of());

		when(clienteRepositorio.findBySalonIdAndTelefono(
				1L, "3001112233"))
				.thenReturn(Optional.empty());

		when(clienteRepositorio.saveAndFlush(any(Cliente.class)))
				.thenAnswer(inv -> {
					Cliente cliente = inv.getArgument(0);
					cliente.setId(30L);
					return cliente;
				});

		when(citaRepositorio.saveAndFlush(any(Cita.class)))
				.thenAnswer(inv -> {
					Cita cita = inv.getArgument(0);
					cita.setId(200L);
					return cita;
				});

		RespuestaCita respuesta = citaServicio.crearReservaPublica(
				"barberia-pruebas", solicitud);

		assertEquals(200L, respuesta.id());
		assertEquals(inicio, respuesta.inicio());
		assertEquals(inicio.plusMinutes(30), respuesta.fin());
		assertEquals(EstadoCita.PENDIENTE, respuesta.estado());
		assertEquals(OrigenCita.EN_LINEA, respuesta.origen());
		assertEquals(25000, respuesta.precio());

		verify(clienteRepositorio).saveAndFlush(any(Cliente.class));
		verify(citaRepositorio).saveAndFlush(any(Cita.class));
	}

	@Test
	void reservaPublicaReutilizaClienteExistente() {
		salon.setSlug("barberia-pruebas");

		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 11, 0);

		Cliente existente = new Cliente();
		existente.setId(31L);
		existente.setNombre("Laura Gomez");
		existente.setTelefono("3001112233");
		existente.setSalon(salon);

		when(salonRepositorio.findBySlug("barberia-pruebas"))
				.thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L))
				.thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(
				20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), any(), any()))
				.thenReturn(List.of());
		when(clienteRepositorio.findBySalonIdAndTelefono(
				1L, "3001112233"))
				.thenReturn(Optional.of(existente));
		when(citaRepositorio.saveAndFlush(any(Cita.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		RespuestaCita respuesta = citaServicio.crearReservaPublica(
				"barberia-pruebas",
				solicitudEn(inicio, 10L, 20L));

		assertEquals("Laura Gomez", respuesta.clienteNombre());
		assertEquals(EstadoCita.PENDIENTE, respuesta.estado());
		assertEquals(OrigenCita.EN_LINEA, respuesta.origen());

		verify(clienteRepositorio, never())
				.saveAndFlush(any(Cliente.class));
		verify(citaRepositorio).saveAndFlush(any(Cita.class));
	}

	@Test
	void reservaPublicaRechazaCruceSinGuardar() {
		salon.setSlug("barberia-pruebas");

		LocalDateTime inicio = LocalDateTime.of(2026, 11, 26, 10, 0);

		Cita existente = new Cita();
		existente.setInicio(LocalDateTime.of(2026, 11, 26, 10, 15));
		existente.setFin(LocalDateTime.of(2026, 11, 26, 10, 45));
		existente.setEstado(EstadoCita.CONFIRMADA);

		when(salonRepositorio.findBySlug("barberia-pruebas"))
				.thenReturn(Optional.of(salon));
		when(servicioRepositorio.findByIdAndSalonId(10L, 1L))
				.thenReturn(Optional.of(servicio));
		when(usuarioRepositorio.buscarEstilistaConServiciosPorId(
				20L, 1L, Rol.ESTILISTA))
				.thenReturn(Optional.of(estilista));
		when(citaRepositorio.buscarCitasActivasEstilistaEnRango(
				eq(1L), eq(20L), any(), any(), any()))
				.thenReturn(List.of(existente));

		assertThrows(
				ExcepcionConflicto.class,
				() -> citaServicio.crearReservaPublica(
						"barberia-pruebas",
						solicitudEn(inicio, 10L, 20L)));

		verifyNoInteractions(clienteRepositorio);
		verify(citaRepositorio, never())
				.saveAndFlush(any(Cita.class));
	}

	private SolicitudCrearCita solicitudEn(LocalDateTime inicio, Long servicioId, Long estilistaId) {
		return new SolicitudCrearCita(
				"Carlos Pérez",
				"3001112233",
				servicioId,
				estilistaId,
				inicio,
				null);
	}

	private Cita crearCitaPersistida(Long id, EstadoCita estado) {
		Cliente cliente = new Cliente();
		cliente.setId(30L);
		cliente.setNombre("Carlos Pérez");
		cliente.setTelefono("3001112233");
		cliente.setSalon(salon);

		Cita cita = new Cita();
		cita.setId(id);
		cita.setSalon(salon);
		cita.setCliente(cliente);
		cita.setEstilista(estilista);
		cita.setServicio(servicio);
		cita.setInicio(LocalDateTime.of(2026, 11, 26, 10, 0));
		cita.setFin(LocalDateTime.of(2026, 11, 26, 10, 30));
		cita.setPrecio(25000);
		cita.setEstado(estado);
		cita.setOrigen(OrigenCita.SALON);
		cita.setFechaCreacion(LocalDateTime.of(2026, 11, 25, 10, 0, 0, 100_000_000));
		cita.setFechaActualizacion(LocalDateTime.of(2026, 11, 25, 10, 0, 0, 100_000_000));
		return cita;
	}

}

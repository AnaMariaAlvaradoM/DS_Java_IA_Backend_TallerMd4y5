package com.devsenior.sillon.cita.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devsenior.sillon.cita.dto.RespuestaCita;
import com.devsenior.sillon.cita.dto.SolicitudCambiarEstadoCita;
import com.devsenior.sillon.cita.dto.SolicitudCrearCita;
import com.devsenior.sillon.cita.modelo.EstadoCita;
import com.devsenior.sillon.cita.modelo.OrigenCita;
import com.devsenior.sillon.cita.servicio.CitaServicio;
import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionNoPermitida;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ManejadorExcepciones;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

class CitaControladorTest {

	private CitaServicio citaServicio;
	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		citaServicio = mock(CitaServicio.class);
		Clock reloj = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), ZoneId.of("America/Bogota"));

		mockMvc = MockMvcBuilders.standaloneSetup(new CitaControlador(citaServicio))
				.setControllerAdvice(new ManejadorExcepciones(reloj))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();

		autenticarComo(5L, 1L, Rol.DUENO);
	}

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void crearCitaValidaResponde201ConContratoYFormatoFecha() throws Exception {
		when(citaServicio.crear(eq(1L), any(SolicitudCrearCita.class)))
				.thenReturn(respuestaEjemplo(EstadoCita.CONFIRMADA));

		mockMvc.perform(post("/api/citas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "clienteNombre": "Carlos Pérez",
						  "clienteTelefono": "+573001112233",
						  "servicioId": 10,
						  "estilistaId": 20,
						  "inicio": "2026-11-26T10:00:00",
						  "nota": "Sin espera"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(100))
				.andExpect(jsonPath("$.inicio").value("2026-11-26T10:00:00"))
				.andExpect(jsonPath("$.fin").value("2026-11-26T10:30:00"))
				.andExpect(jsonPath("$.estado").value("CONFIRMADA"))
				.andExpect(jsonPath("$.origen").value("SALON"))
				.andExpect(jsonPath("$.precio").value(25000))
				.andExpect(jsonPath("$.servicioId").value(10))
				.andExpect(jsonPath("$.servicioNombre").value("Corte clásico"))
				.andExpect(jsonPath("$.estilistaId").value(20))
				.andExpect(jsonPath("$.estilistaNombre").value("Mateo"))
				.andExpect(jsonPath("$.clienteNombre").value("Carlos Pérez"))
				.andExpect(jsonPath("$.clienteTelefono").value("+573001112233"))
				.andExpect(jsonPath("$.nota").value("Sin espera"))
				.andExpect(jsonPath("$.fechaCreacion").value("2026-11-25T10:00:00"))
				.andExpect(jsonPath("$.fechaActualizacion").value("2026-11-25T10:00:01"));
	}

	@Test
	void crearCitaConDatosInvalidosResponde400SinInvocarServicio() throws Exception {
		mockMvc.perform(post("/api/citas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "clienteNombre": "",
						  "clienteTelefono": "123",
						  "servicioId": null,
						  "estilistaId": 20,
						  "inicio": "2026-11-26T10:00:00"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400));

		verifyNoInteractions(citaServicio);
	}

	@Test
	void crearCitaConConflictoResponde409() throws Exception {
		when(citaServicio.crear(eq(1L), any(SolicitudCrearCita.class)))
				.thenThrow(new ExcepcionConflicto("El horario solicitado no está disponible."));

		mockMvc.perform(post("/api/citas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "clienteNombre": "Carlos Pérez",
						  "clienteTelefono": "+573001112233",
						  "servicioId": 10,
						  "estilistaId": 20,
						  "inicio": "2026-11-26T10:00:00"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(409));
	}

	@Test
	void listarCitasConYSinFechaResponde200YFechaInvalida400() throws Exception {
		when(citaServicio.listarPorFecha(eq(1L), eq(5L), eq(Rol.DUENO), isNull()))
				.thenReturn(List.of(respuestaEjemplo(EstadoCita.CONFIRMADA)));

		mockMvc.perform(get("/api/citas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(100));

		autenticarComo(20L, 1L, Rol.ESTILISTA);
		when(citaServicio.listarPorFecha(1L, 20L, Rol.ESTILISTA, LocalDate.of(2026, 11, 26)))
				.thenReturn(List.of(respuestaEjemplo(EstadoCita.CONFIRMADA)));

		mockMvc.perform(get("/api/citas").param("fecha", "2026-11-26"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].estilistaId").value(20));

		mockMvc.perform(get("/api/citas").param("fecha", "26-11-2026"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400));
	}

	@Test
	void cambiarEstadoResponde200_400_403_404_y_409() throws Exception {
		when(citaServicio.cambiarEstado(
				eq(1L), eq(5L), eq(Rol.DUENO), eq(100L), any(SolicitudCambiarEstadoCita.class)))
				.thenReturn(respuestaEjemplo(EstadoCita.COMPLETADA));

		mockMvc.perform(patch("/api/citas/100/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "estado": "COMPLETADA"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("COMPLETADA"));

		mockMvc.perform(patch("/api/citas/100/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "estado": "INVALIDO"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400));

		autenticarComo(20L, 1L, Rol.ESTILISTA);
		when(citaServicio.cambiarEstado(
				eq(1L), eq(20L), eq(Rol.ESTILISTA), eq(100L), any(SolicitudCambiarEstadoCita.class)))
				.thenThrow(new ExcepcionNoPermitida("No permitido."));

		mockMvc.perform(patch("/api/citas/100/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "estado": "CANCELADA"
						}
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.codigo").value(403));

		when(citaServicio.cambiarEstado(
				eq(1L), eq(20L), eq(Rol.ESTILISTA), eq(999L), any(SolicitudCambiarEstadoCita.class)))
				.thenThrow(new ExcepcionRecursoNoEncontrado("Cita no encontrada."));

		mockMvc.perform(patch("/api/citas/999/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "estado": "COMPLETADA"
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value(404));

		when(citaServicio.cambiarEstado(
				eq(1L), eq(20L), eq(Rol.ESTILISTA), eq(101L), any(SolicitudCambiarEstadoCita.class)))
				.thenThrow(new ExcepcionConflicto("Transición inválida."));

		mockMvc.perform(patch("/api/citas/101/estado")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "estado": "COMPLETADA"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(409));
	}

	private void autenticarComo(Long usuarioId, Long salonId, Rol rol) {
		Salon salon = new Salon();
		salon.setId(salonId);
		Usuario usuario = new Usuario();
		usuario.setId(usuarioId);
		usuario.setEmail("usuario@salon.example");
		usuario.setPassword("hash");
		usuario.setRol(rol);
		usuario.setActivo(true);
		usuario.setSalon(salon);

		UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	private RespuestaCita respuestaEjemplo(EstadoCita estado) {
		return new RespuestaCita(
				100L,
				LocalDateTime.of(2026, 11, 26, 10, 0),
				LocalDateTime.of(2026, 11, 26, 10, 30),
				estado,
				OrigenCita.SALON,
				25000,
				10L,
				"Corte clásico",
				20L,
				"Mateo",
				"Carlos Pérez",
				"+573001112233",
				"Sin espera",
				LocalDateTime.of(2026, 11, 25, 10, 0, 0, 123_000_000),
				LocalDateTime.of(2026, 11, 25, 10, 0, 1, 456_000_000));
	}

}

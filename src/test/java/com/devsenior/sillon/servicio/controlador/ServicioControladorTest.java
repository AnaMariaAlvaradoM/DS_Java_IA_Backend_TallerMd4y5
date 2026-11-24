package com.devsenior.sillon.servicio.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ManejadorExcepciones;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.servicio.dto.RespuestaServicio;
import com.devsenior.sillon.servicio.dto.SolicitudServicio;
import com.devsenior.sillon.servicio.servicio.ServicioServicio;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

class ServicioControladorTest {

	private ServicioServicio servicioServicio;
	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		servicioServicio = mock(ServicioServicio.class);
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:30:00Z"), ZoneId.of("America/Bogota"));

		mockMvc = MockMvcBuilders.standaloneSetup(new ServicioControlador(servicioServicio))
				.setControllerAdvice(new ManejadorExcepciones(reloj))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();

		Salon salon = new Salon();
		salon.setId(1L);
		Usuario dueno = new Usuario();
		dueno.setId(5L);
		dueno.setEmail("dueno@salon.example");
		dueno.setPassword("hash");
		dueno.setRol(Rol.DUENO);
		dueno.setActivo(true);
		dueno.setSalon(salon);

		UsuarioPrincipal principal = new UsuarioPrincipal(dueno);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void crearServicioValidoResponde201() throws Exception {
		when(servicioServicio.crear(eq(1L), any(SolicitudServicio.class)))
				.thenReturn(new RespuestaServicio(10L, "Corte clásico", "Tijera", 30, 25000, true));

		mockMvc.perform(post("/api/servicios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Corte clásico",
						  "descripcion": "Tijera",
						  "duracionMinutos": 30,
						  "precio": 25000
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.nombre").value("Corte clásico"))
				.andExpect(jsonPath("$.activo").value(true));
	}

	@Test
	void crearServicioConDuracionYPrecioInvalidosResponde400() throws Exception {
		mockMvc.perform(post("/api/servicios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Corte express",
						  "duracionMinutos": 10,
						  "precio": 0
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"));

		verifyNoInteractions(servicioServicio);
	}

	@Test
	void crearServicioConDuracionMayorA480Responde400() throws Exception {
		mockMvc.perform(post("/api/servicios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Jornada completa",
						  "duracionMinutos": 481,
						  "precio": 100000
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400));

		verifyNoInteractions(servicioServicio);
	}

	@Test
	void actualizarServicioDeOtroSalonResponde404() throws Exception {
		when(servicioServicio.actualizar(eq(1L), eq(99L), any(SolicitudServicio.class)))
				.thenThrow(new ExcepcionRecursoNoEncontrado("Servicio no encontrado."));

		mockMvc.perform(put("/api/servicios/99")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Corte clásico",
						  "duracionMinutos": 30,
						  "precio": 25000,
						  "activo": true
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"));
	}

}

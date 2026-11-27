package com.devsenior.sillon.usuario.controlador;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
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

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ManejadorExcepciones;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.dto.RespuestaEstilista;
import com.devsenior.sillon.usuario.dto.SolicitudEstilista;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;
import com.devsenior.sillon.usuario.servicio.EstilistaServicio;

class EstilistaControladorTest {

	private EstilistaServicio estilistaServicio;
	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		estilistaServicio = mock(EstilistaServicio.class);
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:30:00Z"), ZoneId.of("America/Bogota"));

		mockMvc = MockMvcBuilders.standaloneSetup(new EstilistaControlador(estilistaServicio))
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
	void registrarEstilistaValidoResponde201SinPassword() throws Exception {
		when(estilistaServicio.registrar(eq(1L), any(SolicitudEstilista.class)))
				.thenReturn(new RespuestaEstilista(
						101L,
						"Mateo",
						"mateo@salon.example",
						true,
						List.of("Corte clásico", "Barba completa")));

		mockMvc.perform(post("/api/estilistas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Mateo",
						  "email": "mateo@salon.example",
						  "password": "ClaveMateo2026",
						  "servicioIds": [10, 11]
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(101))
				.andExpect(jsonPath("$.nombre").value("Mateo"))
				.andExpect(jsonPath("$.servicios[0]").value("Corte clásico"))
				.andExpect(jsonPath("$.servicios[1]").value("Barba completa"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void registrarEstilistaSinServiciosOConDatosInvalidosResponde400() throws Exception {
		mockMvc.perform(post("/api/estilistas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Mateo",
						  "email": "correo-invalido",
						  "password": "123",
						  "servicioIds": []
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400));

		verifyNoInteractions(estilistaServicio);
	}

	@Test
	void registrarTercerEstilistaPlanGratisResponde402() throws Exception {
		when(estilistaServicio.registrar(eq(1L), any(SolicitudEstilista.class)))
				.thenThrow(new ExcepcionPagoRequerido("El plan GRATIS permite un máximo de 2 estilistas."));

		mockMvc.perform(post("/api/estilistas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Tercero",
						  "email": "tercero@salon.example",
						  "password": "ClaveTercero2026",
						  "servicioIds": [10]
						}
						"""))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.codigo").value(402));
	}

	@Test
	void registrarConCorreoDuplicadoResponde409() throws Exception {
		when(estilistaServicio.registrar(eq(1L), any(SolicitudEstilista.class)))
				.thenThrow(new ExcepcionConflicto("El correo electrónico ya está registrado."));

		mockMvc.perform(post("/api/estilistas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Mateo",
						  "email": "mateo@salon.example",
						  "password": "ClaveMateo2026",
						  "servicioIds": [10]
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(409));
	}

	@Test
	void registrarConServicioDeOtroSalonResponde404() throws Exception {
		when(estilistaServicio.registrar(eq(1L), any(SolicitudEstilista.class)))
				.thenThrow(new ExcepcionRecursoNoEncontrado("Uno o más servicios no existen en este salón."));

		mockMvc.perform(post("/api/estilistas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nombre": "Mateo",
						  "email": "mateo@salon.example",
						  "password": "ClaveMateo2026",
						  "servicioIds": [999]
						}
						"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value(404));
	}

}

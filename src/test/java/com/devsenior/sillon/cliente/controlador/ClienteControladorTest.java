package com.devsenior.sillon.cliente.controlador;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devsenior.sillon.cliente.dto.RespuestaCliente;
import com.devsenior.sillon.cliente.servicio.ClienteServicio;
import com.devsenior.sillon.compartido.excepcion.ManejadorExcepciones;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

class ClienteControladorTest {

	private ClienteServicio clienteServicio;
	private MockMvc mockMvc;

	@BeforeEach
	void preparar() {
		clienteServicio = mock(ClienteServicio.class);
		Clock reloj = Clock.fixed(Instant.parse("2026-11-18T15:30:00Z"), ZoneId.of("America/Bogota"));

		mockMvc = MockMvcBuilders.standaloneSetup(new ClienteControlador(clienteServicio))
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
	void listarClientesResponde200ConCamposDelContrato() throws Exception {
		when(clienteServicio.listar(1L)).thenReturn(List.of(
				new RespuestaCliente(20L, "Laura Gómez", "+573001112233")));

		mockMvc.perform(get("/api/clientes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(20))
				.andExpect(jsonPath("$[0].nombre").value("Laura Gómez"))
				.andExpect(jsonPath("$[0].telefono").value("+573001112233"));

		verify(clienteServicio).listar(1L);
	}

}

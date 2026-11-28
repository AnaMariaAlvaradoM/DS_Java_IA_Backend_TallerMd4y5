package com.devSenior.sillon.auth.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devSenior.sillon.usuario.repositorio.UsuarioRepositorio;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSalonIntegracionTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepositorio usuarioRepositorio;

	@Test
	void registroLoginYSalonCumplenElContrato() throws Exception {
		String marca = String.valueOf(System.currentTimeMillis());
		String correo = "dueno-" + marca + "@sillon.test";
		String cuerpo = """
				{"salonNombre":"Sillon %s","ciudad":"Bogota","direccion":"Calle 10","telefono":"3001112233","nombre":"Ana","email":"%s","password":"clave1234"}
				""".formatted(marca, correo);

		MvcResult registro = mockMvc.perform(post("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.expiraEnMinutos").value(480))
				.andExpect(jsonPath("$.rol").value("DUENO"))
				.andExpect(jsonPath("$.salonSlug").value("sillon-" + marca))
				.andReturn();

		String token = com.jayway.jsonpath.JsonPath.read(registro.getResponse().getContentAsString(), "$.token");
		org.junit.jupiter.api.Assertions.assertTrue(
				usuarioRepositorio.findByEmail(correo).orElseThrow().getPassword().startsWith("$2"));

		mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(409));

		mockMvc.perform(post("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"salonNombre":"Otro","ciudad":"Cali","direccion":"Cra 2","telefono":"300","nombre":"Ana","email":"no-es-correo","password":"corta"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("email")))
				.andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("password")));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"incorrecta"}
						""".formatted(correo)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value(401))
				.andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));

		mockMvc.perform(get("/api/salon"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.codigo").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.mensaje").exists())
				.andExpect(jsonPath("$.fecha").exists());

		mockMvc.perform(get("/api/salon").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan").value("GRATIS"))
				.andExpect(jsonPath("$.creditosIa").value(10))
				.andExpect(jsonPath("$.configuracion.abreDomingos").value(false))
				.andExpect(jsonPath("$.configuracion.intervaloMinutos").value(30));

		mockMvc.perform(put("/api/salon/configuracion")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"horaApertura":"19:00","horaCierre":"08:00","abreDomingos":true,"intervaloMinutos":30}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value(409));

		mockMvc.perform(put("/api/salon/configuracion")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"horaApertura":"09:00","horaCierre":"18:00","abreDomingos":true,"intervaloMinutos":10}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("intervaloMinutos")));
	}

}

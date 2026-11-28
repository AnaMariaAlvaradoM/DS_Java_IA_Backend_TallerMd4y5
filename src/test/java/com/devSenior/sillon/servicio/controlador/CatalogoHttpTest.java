package com.devSenior.sillon.servicio.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@SpringBootTest
@AutoConfigureMockMvc
class CatalogoHttpTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void catalogoEstilistasYUsuariosRespetanPermisos() throws Exception {
		String marca = String.valueOf(System.currentTimeMillis());
		String tokenDueno = tokenDeRegistro("Dueno " + marca, "dueno-" + marca + "@sillon.test");

		mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(servicio("Corte clasico", 5, 35000)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(servicio("Corte clasico", 45, -1)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"" + "N".repeat(81) + "\",\"duracionMinutos\":45,\"precio\":35000}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Corte\",\"descripcion\":\"" + "D".repeat(301)
						+ "\",\"duracionMinutos\":45,\"precio\":35000}"))
				.andExpect(status().isBadRequest());

		MvcResult corte = mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(servicio("Corte clasico", 45, 35000)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.activo").value(true))
				.andReturn();
		MvcResult barba = mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(servicio("Arreglo de barba", 30, 20000)))
				.andExpect(status().isCreated())
				.andReturn();
		int corteId = com.jayway.jsonpath.JsonPath.read(corte.getResponse().getContentAsString(), "$.id");
		int barbaId = com.jayway.jsonpath.JsonPath.read(barba.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(estilista("Mateo", "mateo-" + marca + "@sillon.test", corteId + "," + barbaId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.servicios.length()").value(2));
		mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(estilista("Sara", "sara-" + marca + "@sillon.test", String.valueOf(corteId))))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(estilista("Luis", "luis-" + marca + "@sillon.test", String.valueOf(corteId))))
				.andExpect(status().isPaymentRequired());
		mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(estilista("Mateo", "mateo-" + marca + "@sillon.test", String.valueOf(corteId))))
				.andExpect(status().isPaymentRequired());

		String tokenEstilista = tokenDeLogin("sara-" + marca + "@sillon.test");
		mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + tokenEstilista))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/usuarios/yo").header("Authorization", "Bearer " + tokenEstilista))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("sara-" + marca + "@sillon.test"))
				.andExpect(jsonPath("$.password").doesNotExist());
		mockMvc.perform(put("/api/salon").header("Authorization", "Bearer " + tokenEstilista)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Otro\",\"ciudad\":\"Cali\",\"direccion\":\"Cra 1\",\"telefono\":\"300\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/salon").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Salon " + marca + "\",\"ciudad\":\"Medellin\",\"direccion\":\"Cra 10\",\"telefono\":\"3009998877\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ciudad").value("Medellin"));
		mockMvc.perform(put("/api/salon/configuracion").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"horaApertura\":\"20:00\",\"horaCierre\":\"09:00\",\"abreDomingos\":false,\"intervaloMinutos\":30}"))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/servicios").header("Authorization", "Bearer " + tokenEstilista).param("activo", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		String tokenOtro = tokenDeRegistro("Otro " + marca, "otro-" + marca + "@sillon.test");
		mockMvc.perform(put("/api/servicios/" + corteId).header("Authorization", "Bearer " + tokenOtro)
				.contentType(MediaType.APPLICATION_JSON)
				.content(servicio("Ajeno", 45, 1000)))
				.andExpect(status().isNotFound());

		MvcResult dueno = mockMvc.perform(get("/api/usuarios/yo").header("Authorization", "Bearer " + tokenDueno))
				.andExpect(status().isOk())
				.andReturn();
		int duenoId = com.jayway.jsonpath.JsonPath.read(dueno.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(patch("/api/usuarios/" + duenoId + "/activo").header("Authorization", "Bearer " + tokenOtro)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"activo\":false}"))
				.andExpect(status().isNotFound());
	}

	private String tokenDeRegistro(String salon, String email) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/registro")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"salonNombre":"%s","ciudad":"Bogota","direccion":"Calle 1","telefono":"3001112233","nombre":"Ana","email":"%s","password":"clave1234"}
						""".formatted(salon, email)))
				.andExpect(status().isCreated())
				.andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private String tokenDeLogin(String email) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"clave1234"}
						""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.token");
	}

	private String servicio(String nombre, int minutos, int precio) {
		return "{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":" + minutos + ",\"precio\":" + precio + "}";
	}

	private String estilista(String nombre, String email, String servicioIds) {
		return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"password\":\"clave1234\",\"servicioIds\":["
				+ servicioIds + "]}";
	}

}

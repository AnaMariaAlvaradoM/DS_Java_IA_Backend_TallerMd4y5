package com.devSenior.sillon.publico.controlador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AgendaPublicaIntegracionTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void citasReservasYAislamiento() throws Exception {
		String marca = String.valueOf(System.currentTimeMillis());
		String tokenDueno = tokenDeRegistro("Agenda " + marca, "agenda-" + marca + "@sillon.test");
		MvcResult salon = mockMvc.perform(get("/api/salon").header("Authorization", "Bearer " + tokenDueno))
				.andExpect(status().isOk()).andReturn();
		String slug = com.jayway.jsonpath.JsonPath.read(salon.getResponse().getContentAsString(), "$.slug");
		int servicioId = idDe(mockMvc.perform(post("/api/servicios").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Corte\",\"duracionMinutos\":45,\"precio\":35000}"))
				.andExpect(status().isCreated()).andReturn());
		int estilistaId = idDe(mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Mateo\",\"email\":\"mateo-" + marca
						+ "@sillon.test\",\"password\":\"clave1234\",\"servicioIds\":[" + servicioId + "]}"))
				.andExpect(status().isCreated()).andReturn());
		int saraId = idDe(mockMvc.perform(post("/api/estilistas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Sara\",\"email\":\"sara-" + marca
						+ "@sillon.test\",\"password\":\"clave1234\",\"servicioIds\":[" + servicioId + "]}"))
				.andExpect(status().isCreated()).andReturn());

		mockMvc.perform(post("/api/clientes").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana\",\"telefono\":\"3001112233\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/clientes").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana\",\"telefono\":\"300 111 2233\"}"))
				.andExpect(status().isConflict());

		String cita = cita(servicioId, estilistaId, "2026-12-01T10:00:00", "3002223344");
		MvcResult creada = mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON).content(cita))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("CONFIRMADA"))
				.andExpect(jsonPath("$.origen").value("SALON"))
				.andExpect(jsonPath("$.fin").value("2026-12-01T10:45:00"))
				.andExpect(jsonPath("$.precio").value(35000))
				.andReturn();
		int citaId = idDe(creada);
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON).content(cita))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2026-12-01T11:00:00", "3002223344")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2026-12-06T10:00:00", "3002223344")))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2026-12-01T10:15:00", "3002223344")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2020-01-06T10:00:00", "3002223344")))
				.andExpect(status().isConflict());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2027-06-01T10:00:00", "3002223344")))
				.andExpect(status().isBadRequest());

		String tokenSara = tokenDeLogin("sara-" + marca + "@sillon.test");
		mockMvc.perform(get("/api/citas/" + citaId).header("Authorization", "Bearer " + tokenSara))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/citas/" + citaId + "/estado").header("Authorization", "Bearer " + tokenSara)
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(patch("/api/citas/" + citaId + "/estado").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CANCELADA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CANCELADA"));
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenDueno)
				.contentType(MediaType.APPLICATION_JSON).content(cita))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/publico/salones/" + slug))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.estilistas[0].email").doesNotExist());
		mockMvc.perform(post("/api/publico/salones/" + slug + "/reservas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, estilistaId, "2026-12-01T12:00:00", "3005556677")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andExpect(jsonPath("$.origen").value("EN_LINEA"))
				.andExpect(jsonPath("$.fin").value("2026-12-01T12:45:00"))
				.andExpect(jsonPath("$.precio").value(35000));
		mockMvc.perform(get("/api/publico/salones/" + slug + "/disponibilidad")
				.param("servicioId", String.valueOf(servicioId))
				.param("estilistaId", String.valueOf(estilistaId))
				.param("fecha", "2026-12-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:00:00"))))
				.andExpect(jsonPath("$.horariosLibres", org.hamcrest.Matchers.hasItem("13:00:00")));

		String tokenOtro = tokenDeRegistro("Otro " + marca, "otro-agenda-" + marca + "@sillon.test");
		mockMvc.perform(get("/api/citas/" + citaId).header("Authorization", "Bearer " + tokenOtro))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/citas").header("Authorization", "Bearer " + tokenOtro)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cita(servicioId, saraId, "2026-12-02T10:00:00", "3001112233")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/clientes").header("Authorization", "Bearer " + tokenOtro)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Ana\",\"telefono\":\"3001112233\"}"))
				.andExpect(status().isCreated());
	}

	private int idDe(MvcResult resultado) throws Exception {
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String cita(int servicioId, int estilistaId, String inicio, String telefono) {
		return "{\"clienteNombre\":\"Ana\",\"clienteTelefono\":\"" + telefono + "\",\"servicioId\":" + servicioId
				+ ",\"estilistaId\":" + estilistaId + ",\"inicio\":\"" + inicio + "\"}";
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

}

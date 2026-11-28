package com.devSenior.sillon.compartido.manejador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.compartido.config.RelojConfig;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = ManejadorErroresTest.ControladorPruebaErrores.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		ManejadorErrores.class,
		RelojConfig.class,
		ManejadorErroresTest.ControladorPruebaErrores.class
})
class ManejadorErroresTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unErrorForzadoDevuelveLosCuatroCampos() throws Exception {
		mockMvc.perform(get("/prueba-errores/no-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.mensaje").value("El salón no existe"))
				.andExpect(jsonPath("$.fecha").exists());
	}

	@Test
	void laValidacionListaLosCamposSeparadosPorPuntoYComa() throws Exception {
		mockMvc.perform(post("/prueba-errores/validacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value(400))
				.andExpect(jsonPath("$.mensaje")
						.value(org.hamcrest.Matchers.containsString("nombre:")));
	}

	@Test
	void unJsonMalFormadoResponde400() throws Exception {
		mockMvc.perform(post("/prueba-errores/validacion")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El cuerpo JSON está mal formado"));
	}

	@Test
	void unParametroFaltanteResponde400() throws Exception {
		mockMvc.perform(get("/prueba-errores/parametro"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("Falta el parámetro fecha"));
	}

	@Test
	void unParametroConTipoInvalidoResponde400() throws Exception {
		mockMvc.perform(get("/prueba-errores/tipo").param("id", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje")
						.value("El parámetro id tiene un tipo inválido"));
	}

	@RestController
	public static class ControladorPruebaErrores {

		@GetMapping("/prueba-errores/no-encontrado")
		public void noEncontrado() {
			throw new NoEncontradoException("El salón no existe");
		}

		@PostMapping("/prueba-errores/validacion")
		public void validar(@Valid @RequestBody CuerpoPrueba cuerpo) {
		}

		@GetMapping("/prueba-errores/parametro")
		public void parametro(@RequestParam String fecha) {
		}

		@GetMapping("/prueba-errores/tipo")
		public void tipo(@RequestParam Long id) {
		}
	}

	public static class CuerpoPrueba {

		@NotBlank
		private String nombre;

		public String getNombre() {
			return nombre;
		}

		public void setNombre(String nombre) {
			this.nombre = nombre;
		}
	}
}
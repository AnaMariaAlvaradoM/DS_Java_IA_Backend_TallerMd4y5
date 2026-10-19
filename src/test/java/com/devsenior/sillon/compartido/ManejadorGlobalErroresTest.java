package com.devsenior.sillon.compartido;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devsenior.sillon.compartido.excepcion.AccionNoPermitidaException;
import com.devsenior.sillon.compartido.excepcion.CredencialesInvalidasException;
import com.devsenior.sillon.compartido.excepcion.ExcepcionSillon;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.ProveedorNoDisponibleException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class ManejadorGlobalErroresTest {
    private MockMvc mvc;
    private final ControladorPrueba controlador = new ControladorPrueba();

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-20T02:30:00Z"), ZoneId.of("America/Bogota"));
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new ManejadorGlobalErrores(new FabricaErrorApi(reloj)))
                .build();
    }

    static Stream<Arguments> erroresDeNegocio() {
        return Stream.of(
                Arguments.of(new RecursoNoEncontradoException("Recurso no encontrado"), 404),
                Arguments.of(new ReglaNegocioException("Horario inválido"), 409),
                Arguments.of(new PagoRequeridoException("Sin créditos"), 402),
                Arguments.of(new AccionNoPermitidaException("Acción no permitida"), 403),
                Arguments.of(new CredencialesInvalidasException("Credenciales inválidas"), 401),
                Arguments.of(new FirmaInvalidaException("Firma inválida"), 400),
                Arguments.of(new PasarelaException("Respuesta inválida"), 502),
                Arguments.of(new ProveedorNoDisponibleException("Proveedores indisponibles"), 503));
    }

    @ParameterizedTest
    @MethodSource("erroresDeNegocio")
    void conservaCodigoMensajeYFechaDelNegocio(ExcepcionSillon excepcion, int codigo) throws Exception {
        controlador.excepcion = excepcion;
        mvc.perform(get("/prueba/negocio"))
                .andExpect(status().is(codigo))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.error").value(HttpStatus.valueOf(codigo).getReasonPhrase()))
                .andExpect(jsonPath("$.mensaje").value(excepcion.getMessage()))
                .andExpect(jsonPath("$.fecha").value("2026-10-19T21:30:00"));
    }

    @Test
    void validacionNombraCamposYSeparaConPuntoYComa() throws Exception {
        mvc.perform(post("/prueba/datos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"ciudad\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("ciudad: es obligatoria; nombre: es obligatorio"));
    }

    @Test
    void jsonMalFormadoEs400() throws Exception {
        mvc.perform(post("/prueba/datos").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    void parametroFaltanteEs400ConSuNombre() throws Exception {
        mvc.perform(get("/prueba/parametro"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Falta el parámetro obligatorio: cantidad"));
    }

    @Test
    void tipoInvalidoEs400() throws Exception {
        mvc.perform(get("/prueba/parametro").param("cantidad", "texto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @RestController
    static class ControladorPrueba {
        private ExcepcionSillon excepcion;

        @GetMapping("/prueba/negocio")
        public void negocio() { throw excepcion; }

        @PostMapping("/prueba/datos")
        public void datos(@Valid @RequestBody Datos datos) { }

        @GetMapping("/prueba/parametro")
        public int parametro(@RequestParam int cantidad) { return cantidad; }
    }

    record Datos(@NotBlank(message = "es obligatorio") String nombre,
                 @NotBlank(message = "es obligatoria") String ciudad) { }
}

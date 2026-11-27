
package com.devsenior.sillon.publico.controlador;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ManejadorExcepciones;
import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaDisponibilidad;
import com.devsenior.sillon.publico.dto.RespuestaEstilistaPublico;
import com.devsenior.sillon.publico.dto.RespuestaServicioPublico;
import com.devsenior.sillon.publico.servicio.PublicoServicio;

class PublicoControladorTest {

    private PublicoServicio publicoServicio;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        publicoServicio = mock(PublicoServicio.class);

        Clock reloj = Clock.fixed(
                Instant.parse("2026-11-24T15:00:00Z"),
                ZoneId.of("America/Bogota"));

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new PublicoControlador(publicoServicio))
                .setControllerAdvice(
                        new ManejadorExcepciones(reloj))
                .build();
    }

    @Test
    void catalogoPublicoDevuelve200YContratoCorrecto()
            throws Exception {

        RespuestaCatalogoPublico catalogo = new RespuestaCatalogoPublico(
                "Barberia Pruebas",
                "barberia-pruebas",
                "Bogota",
                "Calle 100",
                "3001234567",
                "08:00 - 19:00 (lunes a sábado)",
                List.of(
                        new RespuestaServicioPublico(
                                1L, "Corte", "Corte clasico",
                                45, 40000),
                        new RespuestaServicioPublico(
                                2L, "Barba", "Arreglo de barba",
                                30, 20000)),
                List.of(
                        new RespuestaEstilistaPublico(
                                3L, "Mateo",
                                List.of(1L, 2L)),
                        new RespuestaEstilistaPublico(
                                4L, "Sara",
                                List.of(1L))));

        when(publicoServicio.consultarCatalogo(
                "barberia-pruebas"))
                .thenReturn(catalogo);

        mockMvc.perform(
                get("/api/publico/salones/barberia-pruebas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre")
                        .value("Barberia Pruebas"))
                .andExpect(jsonPath("$.slug")
                        .value("barberia-pruebas"))
                .andExpect(jsonPath("$.servicios.length()")
                        .value(2))
                .andExpect(jsonPath("$.estilistas.length()")
                        .value(2))
                .andExpect(jsonPath("$.servicios[0].precio")
                        .value(40000))
                .andExpect(jsonPath("$.estilistas[0].servicioIds[0]")
                        .value(1))
                .andExpect(jsonPath("$.estilistas[0].email")
                        .doesNotExist())
                .andExpect(jsonPath("$.estilistas[0].password")
                        .doesNotExist())
                .andExpect(jsonPath("$.plan")
                        .doesNotExist())
                .andExpect(jsonPath("$.creditosIa")
                        .doesNotExist());
    }

    @Test
    void catalogoDeSalonInexistenteDevuelve404()
            throws Exception {

        when(publicoServicio.consultarCatalogo("no-existe"))
                .thenThrow(new ExcepcionRecursoNoEncontrado(
                        "Salon no encontrado."));

        mockMvc.perform(
                get("/api/publico/salones/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo")
                        .value(404));
    }

    @Test
    void disponibilidadDevuelve200ConHorasExactas()
            throws Exception {

        RespuestaDisponibilidad disponibilidad = new RespuestaDisponibilidad(
                LocalDate.of(2026, 11, 26),
                "Corte",
                "Mateo",
                45,
                List.of(
                        "09:30:00",
                        "10:00:00",
                        "11:00:00"));

        when(publicoServicio.consultarDisponibilidad(
                "barberia-pruebas",
                1L,
                3L,
                LocalDate.of(2026, 11, 26)))
                .thenReturn(disponibilidad);

        mockMvc.perform(
                get("/api/publico/salones/barberia-pruebas/disponibilidad")
                        .param("servicioId", "1")
                        .param("estilistaId", "3")
                        .param("fecha", "2026-11-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha")
                        .value("2026-11-26"))
                .andExpect(jsonPath("$.servicio")
                        .value("Corte"))
                .andExpect(jsonPath("$.estilista")
                        .value("Mateo"))
                .andExpect(jsonPath("$.duracionMinutos")
                        .value(45))
                .andExpect(jsonPath("$.horariosLibres[0]")
                        .value("09:30:00"))
                .andExpect(jsonPath("$.horariosLibres[1]")
                        .value("10:00:00"));
    }

    @Test
    void disponibilidadSinFechaDevuelve400()
            throws Exception {

        mockMvc.perform(
                get("/api/publico/salones/barberia-pruebas/disponibilidad")
                        .param("servicioId", "1")
                        .param("estilistaId", "3"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicoServicio);
    }

    @Test
    void disponibilidadConFechaInvalidaDevuelve400()
            throws Exception {

        mockMvc.perform(
                get("/api/publico/salones/barberia-pruebas/disponibilidad")
                        .param("servicioId", "1")
                        .param("estilistaId", "3")
                        .param("fecha", "fecha-invalida"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(publicoServicio);
    }
}

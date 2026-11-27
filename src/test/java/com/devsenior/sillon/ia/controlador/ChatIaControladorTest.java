
package com.devsenior.sillon.ia.controlador;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaChatIa;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.servicio.ChatIaServicio;

@SpringBootTest
@AutoConfigureMockMvc
class ChatIaControladorTest {

    private static final String SLUG = "salon-prueba";

    private static final String RUTA = "/api/publico/salones/salon-prueba/chat";

    private static final String SOLICITUD_VALIDA = """
            {
              "clienteNombre": "Ana",
              "clienteContacto": "3001234567",
              "historial": [
                {
                  "rol": "CLIENTE",
                  "texto": "Cuanto cuesta el corte?"
                }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatIaServicio chatIaServicio;

    @Test
    void permiteChatPublicoSinAutenticacion()
            throws Exception {

        FacturaIa factura = new FacturaIa(
                "Gemini",
                "gemini-2.5-flash",
                100,
                35,
                BigDecimal.ZERO,
                150L);

        RespuestaChatIa respuesta = new RespuestaChatIa(
                "El corte cuesta 25000 COP.",
                false,
                null,
                factura);

        when(chatIaServicio.responder(
                eq(SLUG),
                any(SolicitudChatIa.class)))
                .thenReturn(respuesta);

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta")
                        .value("El corte cuesta 25000 COP."))
                .andExpect(jsonPath("$.escalado")
                        .value(false))
                .andExpect(jsonPath("$.factura.proveedor")
                        .value("Gemini"))
                .andExpect(jsonPath("$.factura.tokensEntrada")
                        .value(100))
                .andExpect(jsonPath("$.factura.tokensSalida")
                        .value(35));

        verify(chatIaServicio, times(1))
                .responder(
                        eq(SLUG),
                        any(SolicitudChatIa.class));
    }

    @Test
    void rechazaNombreVacioCon400()
            throws Exception {

        String solicitud = SOLICITUD_VALIDA.replace(
                "\"clienteNombre\": \"Ana\"",
                "\"clienteNombre\": \"\"");

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatIaServicio);
    }

    @Test
    void rechazaHistorialVacioCon400()
            throws Exception {

        String solicitud = SOLICITUD_VALIDA.replace(
                """
                        "historial": [
                            {
                              "rol": "CLIENTE",
                              "texto": "Cuanto cuesta el corte?"
                            }
                          ]
                        """,
                "\"historial\": []");

        // Construimos el cuerpo de forma explícita para
        // evitar depender del formato de espacios.
        solicitud = """
                {
                  "clienteNombre": "Ana",
                  "clienteContacto": "3001234567",
                  "historial": []
                }
                """;

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatIaServicio);
    }

    @Test
    void rechazaRolInvalidoCon400()
            throws Exception {

        String solicitud = SOLICITUD_VALIDA.replace(
                "\"CLIENTE\"",
                "\"ADMINISTRADOR\"");

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatIaServicio);
    }

    @Test
    void rechazaHistorialMayorA20MensajesCon400()
            throws Exception {

        String mensajes = IntStream.range(0, 21)
                .mapToObj(indice -> """
                        {
                          "rol": "CLIENTE",
                          "texto": "Mensaje %d"
                        }
                        """.formatted(indice))
                .collect(Collectors.joining(","));

        String solicitud = """
                {
                  "clienteNombre": "Ana",
                  "clienteContacto": "3001234567",
                  "historial": [%s]
                }
                """.formatted(mensajes);

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatIaServicio);
    }

    @Test
    void saldoAgotadoDevuelve402()
            throws Exception {

        when(chatIaServicio.responder(
                eq(SLUG),
                any(SolicitudChatIa.class)))
                .thenThrow(
                        new ExcepcionPagoRequerido(
                                "Sin creditos de IA"));

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void salonInexistenteDevuelve404()
            throws Exception {

        when(chatIaServicio.responder(
                eq(SLUG),
                any(SolicitudChatIa.class)))
                .thenThrow(
                        new ExcepcionRecursoNoEncontrado(
                                "Salon no encontrado"));

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isNotFound());
    }

    @Test
    void respuestaInvalidaDeIaDevuelve502()
            throws Exception {

        when(chatIaServicio.responder(
                eq(SLUG),
                any(SolicitudChatIa.class)))
                .thenThrow(
                        new ExcepcionIntegracion(
                                "La IA devolvio JSON invalido"));

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isBadGateway());
    }

    @Test
    void proveedorNoDisponibleDevuelve503()
            throws Exception {

        when(chatIaServicio.responder(
                eq(SLUG),
                any(SolicitudChatIa.class)))
                .thenThrow(
                        new ExcepcionServicioNoDisponible(
                                "Proveedor IA no disponible"));

        mockMvc.perform(
                post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD_VALIDA))
                .andExpect(status().isServiceUnavailable());
    }
}

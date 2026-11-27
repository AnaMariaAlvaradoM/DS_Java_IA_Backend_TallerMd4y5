
package com.devsenior.sillon.ia.proveedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.servicio.ValidadorJsonIa;
import com.fasterxml.jackson.databind.ObjectMapper;

class ClienteOpenAiCompatibleIaTest {

    private static final String URL =
            "http://localhost:9999/v1/chat/completions";

    private ClienteOpenAiCompatibleIa cliente;
    private MockRestServiceServer servidor;
    private ValidadorJsonIa validador;

    @BeforeEach
    void preparar() {

        ObjectMapper objectMapper = new ObjectMapper();

        RestClient.Builder constructor = RestClient.builder();

        servidor = MockRestServiceServer
                .bindTo(constructor)
                .build();

        validador = new ValidadorJsonIa(objectMapper);

        cliente = new ClienteOpenAiCompatibleIa(
                constructor,
                objectMapper,
                validador,
                URL,
                "llave-ficticia",
                "gemini-prueba");
    }

    @Test
    void generaRecordatorioConFactura() {

        String cuerpo = """
                {
                  "id": "simulada",
                  "object": "chat.completion",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"mensaje\\":\\"Hola Sara, su cita es a las 10:30.\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 120,
                    "completion_tokens": 35
                  }
                }
                """;

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        cuerpo,
                        MediaType.APPLICATION_JSON));

        ResultadoProveedorIa resultado = cliente.generar(
                "Devuelve JSON con la clave mensaje.",
                "Cliente Sara. Hora: 10:30.");

        String mensaje = validador.obtenerTexto(
                validador.interpretar(resultado.contenido()),
                "mensaje");

        assertTrue(mensaje.contains("Sara"));
        assertTrue(mensaje.contains("10:30"));

        assertEquals(
                "Gemini",
                resultado.factura().proveedor());

        assertEquals(
                "gemini-prueba",
                resultado.factura().modelo());

        assertEquals(
                120,
                resultado.factura().tokensEntrada());

        assertEquals(
                35,
                resultado.factura().tokensSalida());

        assertTrue(
                resultado.factura().milisegundos() >= 0);

        servidor.verify();
    }

    @Test
    void generaRespuestaDeChatbot() {

        String cuerpo = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "{\\"respuesta\\":\\"Una persona lo contactará.\\",\\"escalar\\":true}"
                      }
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 90,
                    "completion_tokens": 25
                  }
                }
                """;

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        cuerpo,
                        MediaType.APPLICATION_JSON));

        ResultadoProveedorIa resultado = cliente.generar(
                "Devuelve JSON con respuesta y escalar.",
                "Quiero reservar una cita.");

        var contenido = validador.interpretar(
                resultado.contenido());

        assertEquals(
                "Una persona lo contactará.",
                validador.obtenerTexto(
                        contenido,
                        "respuesta"));

        assertTrue(
                validador.obtenerBooleano(
                        contenido,
                        "escalar"));

        assertEquals(
                90,
                resultado.factura().tokensEntrada());

        servidor.verify();
    }

    @Test
    void rechazaRespuestaSinChoices() {

        String cuerpo = """
                {
                  "choices": [],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 5
                  }
                }
                """;

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        cuerpo,
                        MediaType.APPLICATION_JSON));

        assertThrows(
                ExcepcionIntegracion.class,
                () -> cliente.generar(
                        "Devuelve JSON con mensaje.",
                        "Recordatorio de prueba."));

        servidor.verify();
    }

    @Test
    void rechazaRespuestaSinTokens() {

        String cuerpo = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"mensaje\\":\\"Recordatorio válido\\"}"
                      }
                    }
                  ]
                }
                """;

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        cuerpo,
                        MediaType.APPLICATION_JSON));

        assertThrows(
                ExcepcionIntegracion.class,
                () -> cliente.generar(
                        "Devuelve JSON con mensaje.",
                        "Recordatorio de prueba."));

        servidor.verify();
    }

    @Test
    void errorHttpDelProveedorDevuelveServicioNoDisponible() {

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(
                ExcepcionServicioNoDisponible.class,
                () -> cliente.generar(
                        "Devuelve JSON con mensaje.",
                        "Recordatorio de prueba."));

        servidor.verify();
    }
}

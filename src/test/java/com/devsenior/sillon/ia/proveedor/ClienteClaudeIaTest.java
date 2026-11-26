
package com.devsenior.sillon.ia.proveedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.servicio.ValidadorJsonIa;
import com.fasterxml.jackson.databind.ObjectMapper;

class ClienteClaudeIaTest {

    private static final String URL = "https://api.anthropic.com/v1/messages";

    private static final String MODELO = "claude-sonnet-4-5";

    private static final String LLAVE = "clave-ficticia-para-pruebas";

    private ObjectMapper objectMapper;
    private MockRestServiceServer servidor;
    private RestClient.Builder constructorHttp;

    @BeforeEach
    void configurar() {

        objectMapper = new ObjectMapper();

        constructorHttp = RestClient.builder();

        servidor = MockRestServiceServer
                .bindTo(constructorHttp)
                .build();
    }

    @Test
    void generaRespuestaValidaConFactura()
            throws Exception {

        ClienteClaudeIa cliente = crearCliente(LLAVE);

        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", LLAVE))
                .andExpect(header(
                        "anthropic-version",
                        "2023-06-01"))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(
                        respuestaClaude(
                                """
                                        {"respuesta":"Hola","escalar":false}
                                        """),
                        MediaType.APPLICATION_JSON));

        ResultadoProveedorIa resultado = cliente.generar(
                "Responde exclusivamente JSON",
                "Saluda al cliente");

        assertNotNull(resultado);

        assertTrue(resultado.contenido()
                .contains("\"respuesta\""));

        assertEquals(
                "Claude",
                resultado.factura().proveedor());

        assertEquals(
                MODELO,
                resultado.factura().modelo());

        assertEquals(
                12,
                resultado.factura().tokensEntrada());

        assertEquals(
                9,
                resultado.factura().tokensSalida());

        assertNotNull(
                resultado.factura().costoEstimadoUsd());

        servidor.verify();
    }

    @Test
    void sinClaveNoLlamaAlProveedor() {

        ClienteClaudeIa cliente = crearCliente("");

        assertThrows(
                ExcepcionServicioNoDisponible.class,
                () -> cliente.generar(
                        "Instrucciones",
                        "Mensaje del usuario"));

        servidor.verify();
    }

    @Test
    void errorHttpDevuelveServicioNoDisponible()
            throws Exception {

        ClienteClaudeIa cliente = crearCliente(LLAVE);

        servidor.expect(requestTo(URL))
                .andRespond(withStatus(
                        HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(
                ExcepcionServicioNoDisponible.class,
                () -> cliente.generar(
                        "Instrucciones",
                        "Mensaje del usuario"));

        servidor.verify();
    }

    @Test
    void contenidoGeneradoInvalidoSeEntregaParaValidacionPosterior()
            throws Exception {

        ClienteClaudeIa cliente = crearCliente(LLAVE);

        String contenidoGenerado = "Esta respuesta no es JSON";

        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(
                        respuestaClaude(contenidoGenerado),
                        MediaType.APPLICATION_JSON));

        ResultadoProveedorIa resultado = cliente.generar(
                "Instrucciones",
                "Mensaje del usuario");

        assertNotNull(resultado);

        assertEquals(
                contenidoGenerado,
                resultado.contenido());

        assertEquals(
                "Claude",
                resultado.factura().proveedor());

        assertEquals(
                12,
                resultado.factura().tokensEntrada());

        assertEquals(
                9,
                resultado.factura().tokensSalida());

        servidor.verify();
    }

    @Test
    void ausenciaDeTokensProduceErrorDeIntegracion()
            throws Exception {

        ClienteClaudeIa cliente = crearCliente(LLAVE);

        String cuerpo = objectMapper.writeValueAsString(
                Map.of(
                        "content",
                        List.of(Map.of(
                                "type", "text",
                                "text", "{\"mensaje\":\"Hola\"}")),
                        "usage",
                        Map.of("input_tokens", 12)));

        servidor.expect(requestTo(URL))
                .andRespond(withSuccess(
                        cuerpo,
                        MediaType.APPLICATION_JSON));

        assertThrows(
                ExcepcionIntegracion.class,
                () -> cliente.generar(
                        "Instrucciones",
                        "Mensaje del usuario"));

        servidor.verify();
    }

    private ClienteClaudeIa crearCliente(String llave) {

        return new ClienteClaudeIa(
                constructorHttp,
                objectMapper,
                new ValidadorJsonIa(objectMapper),
                URL,
                llave,
                MODELO);
    }

    private String respuestaClaude(String contenido)
            throws Exception {

        return objectMapper.writeValueAsString(
                Map.of(
                        "content",
                        List.of(Map.of(
                                "type", "text",
                                "text", contenido)),
                        "usage",
                        Map.of(
                                "input_tokens", 12,
                                "output_tokens", 9)));
    }
}

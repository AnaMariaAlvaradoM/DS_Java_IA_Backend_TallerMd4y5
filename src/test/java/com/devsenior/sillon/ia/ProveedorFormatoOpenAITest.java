
package com.devsenior.sillon.ia;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class ProveedorFormatoOpenAITest {

    @Test
    void generarRespuestaConServidorSimulado()
            throws Exception {

        JsonMapper mapper = JsonMapper.builder().build();

        AtomicReference<String> autorizacion =
                new AtomicReference<>();

        AtomicReference<String> solicitudRecibida =
                new AtomicReference<>();

        HttpServer servidor = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        servidor.createContext(
                "/chat/completions",
                intercambio -> {

                    autorizacion.set(
                            intercambio.getRequestHeaders()
                                    .getFirst("Authorization")
                    );

                    solicitudRecibida.set(
                            new String(
                                    intercambio.getRequestBody()
                                            .readAllBytes(),
                                    StandardCharsets.UTF_8
                            )
                    );

                    String respuesta = mapper.writeValueAsString(
                            Map.of(
                                    "model", "gemini-simulado",
                                    "choices", List.of(
                                            Map.of(
                                                    "message",
                                                    Map.of(
                                                            "content",
                                                            "{\"mensaje\":\"Cita confirmada\"}"
                                                    )
                                            )
                                    ),
                                    "usage", Map.of(
                                            "prompt_tokens", 80,
                                            "completion_tokens", 20
                                    )
                            )
                    );

                    byte[] datos = respuesta.getBytes(
                            StandardCharsets.UTF_8
                    );

                    intercambio.getResponseHeaders().set(
                            "Content-Type",
                            "application/json"
                    );

                    intercambio.sendResponseHeaders(
                            200,
                            datos.length
                    );

                    try (var salida =
                            intercambio.getResponseBody()) {
                        salida.write(datos);
                    }
                }
        );

        servidor.start();

        try {
            String url = "http://127.0.0.1:"
                    + servidor.getAddress().getPort();

            var propiedades =
                    new ConfiguracionIA.PropiedadesIA(
                            new ConfiguracionIA.ProveedorOpenAI(
                                    url,
                                    "clave-prueba",
                                    "gemini-simulado"
                            ),
                            null
                    );

            var proveedor = new ProveedorFormatoOpenAI(
                    new FabricaHttpIA(),
                    propiedades,
                    mapper,
                    new CalculadoraCostosIA()
            );

            var resultado = proveedor.generar(
                    "Responde únicamente en JSON",
                    "Genera un recordatorio"
            );

            assertEquals(
                    "{\"mensaje\":\"Cita confirmada\"}",
                    resultado.contenido()
            );

            assertEquals(
                    "Bearer clave-prueba",
                    autorizacion.get()
            );

            var peticion = mapper.readTree(
                    solicitudRecibida.get()
            );

            assertEquals(
                    "gemini-simulado",
                    peticion.path("model").asText()
            );

            assertEquals(
                    "GEMINI",
                    resultado.factura().proveedor()
            );

            assertEquals(
                    80,
                    resultado.factura().tokensEntrada()
            );

            assertEquals(
                    20,
                    resultado.factura().tokensSalida()
            );

        } finally {
            servidor.stop(0);
        }
    }
}

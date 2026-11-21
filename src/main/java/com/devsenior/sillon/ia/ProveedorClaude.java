
package com.devsenior.sillon.ia;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProveedorClaude implements ProveedorIA {

    private final FabricaHttpIA fabricaHttp;
    private final ConfiguracionIA.PropiedadesIA propiedades;
    private final JsonMapper jsonMapper;

        private final CalculadoraCostosIA calculadoraCostos;

        @Value("${sillon.ia.claude.precio-entrada-millon:0}")
        private double precioEntradaPorMillon;

        @Value("${sillon.ia.claude.precio-salida-millon:0}")
        private double precioSalidaPorMillon;



        public ProveedorClaude(
                FabricaHttpIA fabricaHttp,
                ConfiguracionIA.PropiedadesIA propiedades,
                JsonMapper jsonMapper,
                CalculadoraCostosIA calculadoraCostos
        ) {
           this.fabricaHttp = fabricaHttp;
           this.propiedades = propiedades;
           this.jsonMapper = jsonMapper;
           this.calculadoraCostos = calculadoraCostos;
        }

    @Override
    public String nombre() {
        return "CLAUDE";
    }

    @Override
    public Resultado generar(
            String instrucciones,
            String mensaje
    ) {
        var configuracion = propiedades.claude();

        if (configuracion == null
                || configuracion.llave() == null
                || configuracion.llave().isBlank()
                || configuracion.llave().startsWith("REEMPLAZAR")) {
            throw new IllegalStateException(
                    "La clave API de Claude no está configurada"
            );
        }

        String cuerpo = jsonMapper.writeValueAsString(
                Map.of(
                        "model", configuracion.modelo(),
                        "max_tokens", 1024,
                        "system", instrucciones,
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", mensaje
                                )
                        )
                )
        );

        HttpRequest solicitud = fabricaHttp
                .prepararSolicitud(configuracion.url())
                .header(
                        "x-api-key",
                        configuracion.llave()
                )
                .header(
                        "anthropic-version",
                        "2023-06-01"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                cuerpo,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        long inicio = System.nanoTime();

        HttpResponse<String> respuesta;

        try {
            respuesta = fabricaHttp.cliente().send(
                    solicitud,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "La solicitud a Claude fue interrumpida",
                    ex
            );

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible conectar con Claude",
                    ex
            );
        }

        long milisegundos = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - inicio
        );

        if (respuesta.statusCode() < 200
                || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Claude respondió con HTTP "
                            + respuesta.statusCode()
            );
        }

        JsonNode datos = jsonMapper.readTree(
                respuesta.body()
        );

        StringBuilder texto = new StringBuilder();

        for (JsonNode bloque : datos.path("content")) {
            if ("text".equals(
                    bloque.path("type").asText()
            )) {
                texto.append(
                        bloque.path("text").asText("")
                );
            }
        }

        String contenido = texto.toString();

        if (contenido.isBlank()) {
            throw new IllegalStateException(
                    "Claude devolvió una respuesta vacía"
            );
        }

        JsonNode uso = datos.path("usage");

        int tokensEntrada = uso
                .path("input_tokens")
                .asInt(0);

        int tokensSalida = uso
                .path("output_tokens")
                .asInt(0);

        String modelo = datos
                .path("model")
                .asText(configuracion.modelo());


        double costoEstimado = calculadoraCostos.calcular(
                tokensEntrada,
                tokensSalida,
                precioEntradaPorMillon,
                precioSalidaPorMillon
        );

        FacturaIA factura = new FacturaIA(
                nombre(),
                modelo,
                tokensEntrada,
                tokensSalida,
                costoEstimado,
                milisegundos
        );
        return new Resultado(contenido, factura);
    }
}

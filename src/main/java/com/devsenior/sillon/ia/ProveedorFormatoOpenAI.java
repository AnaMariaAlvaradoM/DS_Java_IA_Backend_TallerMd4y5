
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
public class ProveedorFormatoOpenAI implements ProveedorIA {

    private final FabricaHttpIA fabricaHttp;
    private final ConfiguracionIA.PropiedadesIA propiedades;
    private final JsonMapper jsonMapper;

        private final CalculadoraCostosIA calculadoraCostos;

        @Value("${sillon.ia.openai.precio-entrada-millon:0}")
        private double precioEntradaPorMillon;

        @Value("${sillon.ia.openai.precio-salida-millon:0}")
        private double precioSalidaPorMillon;



        public ProveedorFormatoOpenAI(
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
        return "GEMINI";
    }

    @Override
    public Resultado generar(
            String instrucciones,
            String mensaje
    ) {
        var configuracion = propiedades.openai();

        if (configuracion == null
                || configuracion.llave() == null
                || configuracion.llave().isBlank()
                || configuracion.llave().startsWith("REEMPLAZAR")) {
            throw new IllegalStateException(
                    "La clave API de Gemini no está configurada"
            );
        }

        String url = configuracion.url()
                .replaceAll("/+$", "")
                + "/chat/completions";

        String cuerpo = jsonMapper.writeValueAsString(
                Map.of(
                        "model", configuracion.modelo(),
                        "messages", List.of(
                                Map.of(
                                        "role", "system",
                                        "content", instrucciones
                                ),
                                Map.of(
                                        "role", "user",
                                        "content", mensaje
                                )
                        )
                )
        );

        HttpRequest solicitud = fabricaHttp
                .prepararSolicitud(url)
                .header(
                        "Authorization",
                        "Bearer " + configuracion.llave()
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
                    "La solicitud a Gemini fue interrumpida",
                    ex
            );

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible conectar con Gemini",
                    ex
            );
        }

        long milisegundos = TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - inicio
        );

        if (respuesta.statusCode() < 200
                || respuesta.statusCode() >= 300) {
            throw new IllegalStateException(
                    "Gemini respondió con HTTP "
                            + respuesta.statusCode()
            );
        }

        JsonNode datos = jsonMapper.readTree(
                respuesta.body()
        );

        String contenido = datos
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalStateException(
                    "Gemini devolvió una respuesta vacía"
            );
        }

        JsonNode uso = datos.path("usage");

        int tokensEntrada = uso
                .path("prompt_tokens")
                .asInt(0);

        int tokensSalida = uso
                .path("completion_tokens")
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

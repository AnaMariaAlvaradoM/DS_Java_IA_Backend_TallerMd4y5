
package com.devsenior.sillon.ia.proveedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.servicio.ValidadorJsonIa;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ClienteClaudeIa implements ProveedorIa {

    private static final String VERSION_API = "2023-06-01";
    private static final int MAX_TOKENS = 700;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ValidadorJsonIa validadorJson;
    private final String url;
    private final String llave;
    private final String modelo;

    public ClienteClaudeIa(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ValidadorJsonIa validadorJson,
            @Value("${sillon.ia.claude.url:https://api.anthropic.com/v1/messages}") String url,
            @Value("${sillon.ia.claude.llave:}") String llave,
            @Value("${sillon.ia.claude.modelo:claude-sonnet-4-5}") String modelo) {

        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.validadorJson = validadorJson;
        this.url = url;
        this.llave = llave;
        this.modelo = modelo;
    }

    @Override
    public ResultadoProveedorIa generar(
            String instrucciones,
            String mensajeUsuario) {

        if (instrucciones == null
                || instrucciones.isBlank()
                || mensajeUsuario == null
                || mensajeUsuario.isBlank()) {

            throw new ExcepcionIntegracion(
                    "La solicitud al proveedor Claude esta incompleta");
        }

        if (llave == null || llave.isBlank()
                || url == null || url.isBlank()) {

            throw new ExcepcionServicioNoDisponible(
                    "El proveedor Claude no esta configurado");
        }

        Map<String, Object> solicitud = Map.of(
                "model", modelo,
                "max_tokens", MAX_TOKENS,
                "system", instrucciones,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", mensajeUsuario)));

        long inicio = System.nanoTime();

        String cuerpo;

        try {

            cuerpo = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", llave)
                    .header("anthropic-version", VERSION_API)
                    .body(solicitud)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientException excepcion) {

            throw new ExcepcionServicioNoDisponible(
                    "No fue posible comunicarse con Claude");
        }

        long milisegundos = Math.max(
                0L,
                (System.nanoTime() - inicio) / 1_000_000L);

        if (cuerpo == null || cuerpo.isBlank()) {

            throw new ExcepcionIntegracion(
                    "Claude devolvio una respuesta vacia");
        }

        try {

            JsonNode respuesta = objectMapper.readTree(cuerpo);

            if (respuesta == null || !respuesta.isObject()) {
                throw new ExcepcionIntegracion(
                        "Claude devolvio una estructura invalida");
            }

            JsonNode bloques = respuesta.path("content");

            if (!bloques.isArray() || bloques.isEmpty()) {
                throw new ExcepcionIntegracion(
                        "Claude no devolvio contenido");
            }

            String contenido = null;

            for (JsonNode bloque : bloques) {

                if ("text".equals(
                        bloque.path("type").asText())) {

                    contenido = validadorJson.obtenerTexto(
                            bloque,
                            "text");

                    break;
                }
            }

            if (contenido == null) {
                throw new ExcepcionIntegracion(
                        "Claude no devolvio un bloque de texto");
            }

            JsonNode uso = respuesta.path("usage");

            int tokensEntrada = obtenerTokens(
                    uso,
                    "input_tokens");

            int tokensSalida = obtenerTokens(
                    uso,
                    "output_tokens");

            FacturaIa factura = new FacturaIa(
                    "Claude",
                    modelo,
                    tokensEntrada,
                    tokensSalida,
                    BigDecimal.ZERO,
                    milisegundos);

            return new ResultadoProveedorIa(
                    contenido,
                    factura);

        } catch (JsonProcessingException excepcion) {

            throw new ExcepcionIntegracion(
                    "Claude devolvio JSON invalido");
        }
    }

    private int obtenerTokens(
            JsonNode uso,
            String campo) {

        JsonNode valor = uso.path(campo);

        if (!valor.isIntegralNumber()
                || !valor.canConvertToInt()
                || valor.intValue() < 0) {

            throw new ExcepcionIntegracion(
                    "Claude no informo correctamente " + campo);
        }

        return valor.intValue();
    }
}

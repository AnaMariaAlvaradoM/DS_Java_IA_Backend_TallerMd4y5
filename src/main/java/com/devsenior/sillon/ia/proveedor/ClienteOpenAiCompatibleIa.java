
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
public class ClienteOpenAiCompatibleIa implements ProveedorIa {

        private final RestClient restClient;
        private final ObjectMapper objectMapper;
        private final ValidadorJsonIa validadorJson;
        private final String url;
        private final String llave;
        private final String modelo;

        public ClienteOpenAiCompatibleIa(
                        RestClient.Builder restClientBuilder,
                        ObjectMapper objectMapper,
                        ValidadorJsonIa validadorJson,
                        @Value("${sillon.ia.openai.url:}") String url,
                        @Value("${sillon.ia.gemini.llave:}") String llave,
                        @Value("${sillon.ia.gemini.modelo:gemini-2.5-flash}") String modelo) {

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
                                        "La solicitud al proveedor de IA está incompleta");
                }

                if (url == null || url.isBlank()) {
                        throw new ExcepcionServicioNoDisponible(
                                        "No está configurada la URL del proveedor de IA");
                }

                Map<String, Object> solicitud = Map.of(
                                "model", modelo,
                                "messages", List.of(
                                                Map.of(
                                                                "role", "system",
                                                                "content", instrucciones),
                                                Map.of(
                                                                "role", "user",
                                                                "content", mensajeUsuario)));

                long inicio = System.nanoTime();

                String cuerpo;

                try {
                        cuerpo = restClient.post()
                                        .uri(url)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .headers(encabezados -> {
                                                if (llave != null && !llave.isBlank()) {
                                                        encabezados.setBearerAuth(llave);
                                                }
                                        })
                                        .body(solicitud)
                                        .retrieve()
                                        .body(String.class);

                } catch (RestClientException excepcion) {
                        throw new ExcepcionServicioNoDisponible(
                                        "No fue posible comunicarse con el proveedor de IA");
                }

                long milisegundos = Math.max(
                                0L,
                                (System.nanoTime() - inicio) / 1_000_000L);

                if (cuerpo == null || cuerpo.isBlank()) {
                        throw new ExcepcionIntegracion(
                                        "El proveedor de IA devolvió una respuesta vacía");
                }

                try {
                        JsonNode respuesta = objectMapper.readTree(cuerpo);

                        if (respuesta == null || !respuesta.isObject()) {
                                throw new ExcepcionIntegracion(
                                                "La respuesta HTTP del proveedor no es válida");
                        }

                        JsonNode opciones = respuesta.path("choices");

                        if (!opciones.isArray() || opciones.isEmpty()) {
                                throw new ExcepcionIntegracion(
                                                "El proveedor de IA no devolvió opciones");
                        }

                        JsonNode mensaje = opciones.get(0).path("message");

                        String contenido = validadorJson.obtenerTexto(
                                        mensaje,
                                        "content");

                        // No validar aquí el JSON generado por la IA.
                        // El servicio debe consumir el crédito antes
                        // de interpretar y validar ese contenido.

                        JsonNode uso = respuesta.path("usage");

                        int tokensEntrada = obtenerTokens(
                                        uso,
                                        "prompt_tokens");

                        int tokensSalida = obtenerTokens(
                                        uso,
                                        "completion_tokens");

                        FacturaIa factura = new FacturaIa(
                                        "Gemini",
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
                                        "El proveedor de IA devolvió un JSON HTTP inválido");
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
                                        "El proveedor de IA no informó correctamente "
                                                        + campo);
                }

                return valor.intValue();
        }
}

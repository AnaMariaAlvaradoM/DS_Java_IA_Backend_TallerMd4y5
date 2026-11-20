package co.sillon;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import static co.sillon.Fallos.fallo;

@Service
class ProveedorIa {
    record Factura(String proveedor, String modelo, int tokensEntrada, int tokensSalida) {}
    record Resultado(JsonNode contenido, Factura factura) {}
    private final RestClient http;
    private final ObjectMapper json;
    private final String url, clave, modelo;
    final String promptChat, promptRecordatorio;
    ProveedorIa(RestClient http, ObjectMapper json, @Value("${sillon.ia.openai.url}") String url,
                @Value("${sillon.ia.clave}") String clave, @Value("${sillon.ia.modelo}") String modelo) throws IOException {
        this.http=http; this.json=json; this.url=url.replaceFirst("/+$",""); this.clave=clave; this.modelo=modelo;
        this.promptChat=plantilla("chatbot");
        this.promptRecordatorio=plantilla("recordatorio");
    }
    private String plantilla(String nombre) throws IOException {
        return new ClassPathResource("prompts/"+nombre+".txt").getContentAsString(StandardCharsets.UTF_8);
    }
    Resultado consultar(String sistema, List<Map<String,String>> conversacion) {
        if (clave.isBlank()) throw fallo(503,"Configura el proveedor de IA para usar esta función");
        List<Map<String,String>> mensajes=new ArrayList<>();
        mensajes.add(Map.of("role","system","content",sistema));
        mensajes.addAll(conversacion);
        try {
            var resultado=http.post().uri(url+"/chat/completions").headers(h -> h.setBearerAuth(clave))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model",modelo,"messages",mensajes,"response_format",Map.of("type","json_object")))
                    .retrieve().body(JsonNode.class);
            if (resultado==null) throw fallo(502,"Respuesta vacía de IA");
            var cuenta=resultado.path("usage");
            var contenido=json.readTree(resultado.path("choices").path(0).path("message").path("content").asText(""));
            if (contenido==null || !contenido.isObject()) throw fallo(502,"Formato inesperado de IA");
            return new Resultado(contenido,new Factura("Gemini",modelo,cuenta.path("prompt_tokens").asInt(),cuenta.path("completion_tokens").asInt()));
        } catch (RestClientException | IOException e) { throw fallo(502,"No se pudo obtener una respuesta válida de IA"); }
    }
}

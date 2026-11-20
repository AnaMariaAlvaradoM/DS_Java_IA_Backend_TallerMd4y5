package com.devSenior.sillon.ia.proveedor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.ia.config.IaPropiedades;
import com.devSenior.sillon.ia.dto.MensajeIa;
import com.devSenior.sillon.ia.dto.ResultadoIa;
import com.devSenior.sillon.ia.http.FabricaClienteHttp;
import com.devSenior.sillon.ia.json.LectorJson;

import tools.jackson.databind.JsonNode;

@Component
public class ProveedorGemini implements ProveedorIa {

	private static final Logger BITACORA = LoggerFactory.getLogger(ProveedorGemini.class);

	private final IaPropiedades propiedades;
	private final FabricaClienteHttp fabrica;
	private final LectorJson lectorJson;


	public ProveedorGemini(IaPropiedades propiedades, FabricaClienteHttp fabrica, LectorJson lectorJson) {
		this.propiedades = propiedades;
		this.fabrica = fabrica;
		this.lectorJson = lectorJson;
	}

	@Override
	public String nombre() {
		return "gemini";
	}

	@Override
	public ResultadoIa completar(String instrucciones, List<MensajeIa> mensajes) {
		String modelo = propiedades.getOpenai().getModelo();
		if (modelo == null || modelo.isBlank()) {
			BITACORA.error("Modelo de Gemini no configurado");
			throw new ProveedorCaidoException();
		}
		if (!modelo.startsWith("gemini")) {
			BITACORA.error("El modelo configurado en sillon.ia.openai.modelo no parece de Gemini: {}", modelo);
		}
		long inicio = System.nanoTime();
		List<Map<String, String>> chat = new ArrayList<>();
		chat.add(Map.of("role", "system", "content", instrucciones));
		for (MensajeIa mensaje : mensajes) {
			String rol = "assistant".equals(mensaje.rol()) ? "assistant" : "user";
			chat.add(Map.of("role", rol, "content", mensaje.texto()));
		}
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("model", modelo);
		cuerpo.put("messages", chat);
		cuerpo.put("response_format", Map.of("type", "json_object"));
		String respuesta = post(urlChat(), modelo, cuerpo);
		JsonNode nodo = lectorJson.leerObjeto(respuesta);
		JsonNode choices = nodo.get("choices");
		if (choices == null || !choices.isArray() || choices.isEmpty()) {
			throw new IntegracionInvalidaException("La IA no devolvió contenido");
		}
		JsonNode content = choices.get(0).path("message").path("content");
		if (!content.isString() || content.asString().isBlank()) {
			throw new IntegracionInvalidaException("La IA no devolvió contenido");
		}
		int entrada = nodo.path("usage").path("prompt_tokens").asInt(0);
		int salida = nodo.path("usage").path("completion_tokens").asInt(0);
		String modeloRespondido = nodo.path("model").asString(modelo);
		long milis = (System.nanoTime() - inicio) / 1_000_000;
		return new ResultadoIa(content.asString(), factura(modeloRespondido, entrada, salida, milis));
	}

	private String urlChat() {
		String base = propiedades.getOpenai().getUrl();

		if (base == null || base.isBlank()) {
			BITACORA.error("La URL base de Gemini está vacía");
			throw new ProveedorCaidoException();
		}

		String sinBarra = base.endsWith("/")
				? base.substring(0, base.length() - 1)
				: base;

		String url = sinBarra + "/chat/completions";

		try {
			java.net.URI uri = java.net.URI.create(url);

			if (uri.getScheme() == null
					|| (!uri.getScheme().equals("https")
					&& !uri.getScheme().equals("http"))
					|| uri.getHost() == null) {

				BITACORA.error(
						"URL de Gemini inválida. esquema={} host={}",
						uri.getScheme(),
						uri.getHost()
				);

				throw new ProveedorCaidoException();
			}

		} catch (IllegalArgumentException excepcion) {
			BITACORA.error("Formato de URL de Gemini inválido");
			throw new ProveedorCaidoException();
		}

		return url;
	}

	private String post(String url, String modelo, Map<String, Object> cuerpo) {
		try {
			String respuesta = fabrica.crear().post()
					.uri(url)
					.header("Authorization", "Bearer " + propiedades.getGemini().getLlave())
					.header("Content-Type", "application/json")
					.body(cuerpo)
					.retrieve()
					.body(String.class);
			if (respuesta == null || respuesta.isBlank()) {
				throw new ProveedorCaidoException();
			}
			return respuesta;
		} catch (RestClientResponseException excepcion) {
			BITACORA.error("Gemini no disponible. modelo={} estado={}", modelo, excepcion.getStatusCode().value());
			throw new ProveedorCaidoException();
		} catch (ProveedorCaidoException excepcion) {
			throw excepcion;
		} catch (RestClientException excepcion) {
			BITACORA.error("Gemini no disponible. modelo={}", modelo);
			throw new ProveedorCaidoException();
		}
	}

}

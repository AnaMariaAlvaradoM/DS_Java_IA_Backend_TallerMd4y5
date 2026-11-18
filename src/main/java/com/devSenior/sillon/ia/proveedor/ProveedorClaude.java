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
public class ProveedorClaude implements ProveedorIa {

	private static final Logger BITACORA = LoggerFactory.getLogger(ProveedorClaude.class);

	private final IaPropiedades propiedades;
	private final FabricaClienteHttp fabrica;
	private final LectorJson lectorJson;

	public ProveedorClaude(IaPropiedades propiedades, FabricaClienteHttp fabrica, LectorJson lectorJson) {
		this.propiedades = propiedades;
		this.fabrica = fabrica;
		this.lectorJson = lectorJson;
	}

	@Override
	public String nombre() {
		return "claude";
	}

	@Override
	public ResultadoIa completar(String instrucciones, List<MensajeIa> mensajes) {
		String modelo = propiedades.getRespaldo().getModelo();
		if (modelo == null || modelo.isBlank()) {
			BITACORA.error("Modelo de Claude no configurado");
			throw new ProveedorCaidoException();
		}
		if (!modelo.startsWith("claude")) {
			BITACORA.error("El modelo configurado en sillon.ia.respaldo.modelo no parece de Claude: {}", modelo);
		}
		long inicio = System.nanoTime();
		Map<String, Object> cuerpo = new LinkedHashMap<>();
		cuerpo.put("model", modelo);
		cuerpo.put("max_tokens", 1024);
		cuerpo.put("system", instrucciones);
		cuerpo.put("messages", alternar(mensajes));
		String respuesta = post(modelo, cuerpo);
		JsonNode nodo = lectorJson.leerObjeto(respuesta);
		String texto = textoDe(nodo.get("content"));
		if (texto.isBlank()) {
			throw new IntegracionInvalidaException("La IA no devolvió contenido");
		}
		int entrada = nodo.path("usage").path("input_tokens").asInt(0);
		int salida = nodo.path("usage").path("output_tokens").asInt(0);
		String modeloRespondido = nodo.path("model").asString(modelo);
		long milis = (System.nanoTime() - inicio) / 1_000_000;
		return new ResultadoIa(texto, factura(modeloRespondido, entrada, salida, milis));
	}

	static List<Map<String, String>> alternar(List<MensajeIa> mensajes) {
		List<Map<String, String>> chat = new ArrayList<>();
		for (MensajeIa mensaje : mensajes) {
			String rol = "assistant".equals(mensaje.rol()) ? "assistant" : "user";
			if (!chat.isEmpty() && rol.equals(chat.get(chat.size() - 1).get("role"))) {
				Map<String, String> anterior = chat.get(chat.size() - 1);
				anterior.put("content", anterior.get("content") + "\n" + mensaje.texto());
			} else {
				chat.add(new LinkedHashMap<>(Map.of("role", rol, "content", mensaje.texto())));
			}
		}
		if (chat.isEmpty() || !"user".equals(chat.get(0).get("role"))) {
			chat.add(0, new LinkedHashMap<>(Map.of("role", "user", "content", "Responde con el JSON pedido.")));
		}
		return chat;
	}

	private String textoDe(JsonNode content) {
		if (content == null || !content.isArray() || content.isEmpty()) {
			return "";
		}
		StringBuilder texto = new StringBuilder();
		for (JsonNode bloque : content) {
			if ("text".equals(bloque.path("type").asString("")) || bloque.path("text").isString()) {
				texto.append(bloque.path("text").asString(""));
			}
		}
		return texto.toString();
	}

	private String post(String modelo, Map<String, Object> cuerpo) {
		String url = propiedades.getRespaldo().getUrl();
		if (url == null || url.isBlank()) {
			throw new ProveedorCaidoException();
		}
		try {
			String respuesta = fabrica.crear().post()
					.uri(url)
					.header("x-api-key", propiedades.getRespaldo().getLlave())
					.header("anthropic-version", "2023-06-01")
					.header("Content-Type", "application/json")
					.body(cuerpo)
					.retrieve()
					.body(String.class);
			if (respuesta == null || respuesta.isBlank()) {
				throw new ProveedorCaidoException();
			}
			return respuesta;
		} catch (RestClientResponseException excepcion) {
			BITACORA.error("Claude no disponible. modelo={} estado={}", modelo, excepcion.getStatusCode().value());
			throw new ProveedorCaidoException();
		} catch (ProveedorCaidoException excepcion) {
			throw excepcion;
		} catch (RestClientException excepcion) {
			BITACORA.error("Claude no disponible. modelo={}", modelo);
			throw new ProveedorCaidoException();
		}
	}

}

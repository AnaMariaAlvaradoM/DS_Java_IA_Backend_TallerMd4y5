package com.devSenior.sillon.ia.json;

import org.springframework.stereotype.Component;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LectorJson {

	private final JsonMapper mapper = JsonMapper.builder().build();

	public JsonNode leerObjeto(String texto) {
		if (texto == null || texto.isBlank()) {
			throw new IntegracionInvalidaException("La IA no devolvió JSON");
		}
		String limpio = texto.trim();
		if (limpio.startsWith("```")) {
			int salto = limpio.indexOf('\n');
			int cierre = limpio.lastIndexOf("```");
			if (salto >= 0 && cierre > salto) {
				limpio = limpio.substring(salto + 1, cierre).trim();
			}
		}
		int inicio = limpio.indexOf('{');
		int fin = limpio.lastIndexOf('}');
		if (inicio < 0 || fin <= inicio) {
			throw new IntegracionInvalidaException("La IA no devolvió JSON");
		}
		try {
			JsonNode nodo = mapper.readTree(limpio.substring(inicio, fin + 1));
			if (nodo == null || !nodo.isObject()) {
				throw new IntegracionInvalidaException("La IA no devolvió JSON");
			}
			return nodo;
		} catch (IntegracionInvalidaException excepcion) {
			throw excepcion;
		} catch (RuntimeException excepcion) {
			throw new IntegracionInvalidaException("La IA no devolvió JSON");
		}
	}

	public String texto(JsonNode nodo, String clave) {
		JsonNode valor = nodo.get(clave);
		if (valor == null || valor.isNull() || !valor.isString() || valor.asString().isBlank()) {
			throw new IntegracionInvalidaException("La respuesta no incluye " + clave);
		}
		return valor.asString();
	}

	public boolean booleano(JsonNode nodo, String clave) {
		JsonNode valor = nodo.get(clave);
		if (valor == null || !valor.isBoolean()) {
			throw new IntegracionInvalidaException("La respuesta no incluye " + clave);
		}
		return valor.asBoolean();
	}

}

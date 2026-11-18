package com.devSenior.sillon.ia.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;

import tools.jackson.databind.JsonNode;

class LectorJsonTest {

	private final LectorJson lector = new LectorJson();

	@Test
	void leeUnObjetoValido() {
		JsonNode nodo = lector.leerObjeto("texto {\"mensaje\":\"Hola\",\"escalar\":false} fin");
		assertEquals("Hola", lector.texto(nodo, "mensaje"));
		assertTrue(!lector.booleano(nodo, "escalar"));
	}

	@Test
	void rechazaJsonInvalidoOSinClave() {
		assertThrows(IntegracionInvalidaException.class, () -> lector.leerObjeto("no es json"));
		JsonNode nodo = lector.leerObjeto("{\"otro\":1}");
		assertThrows(IntegracionInvalidaException.class, () -> lector.texto(nodo, "mensaje"));
		assertThrows(IntegracionInvalidaException.class, () -> lector.texto(nodo, "respuesta"));
	}

}

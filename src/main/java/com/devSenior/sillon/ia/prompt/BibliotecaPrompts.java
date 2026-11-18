package com.devSenior.sillon.ia.prompt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

	public String cargar(String archivo, Map<String, String> variables) {
		String texto = leer(archivo);
		for (Map.Entry<String, String> variable : variables.entrySet()) {
			String valor = variable.getValue() == null ? "" : variable.getValue();
			texto = texto.replace("{{" + variable.getKey() + "}}", valor);
		}
		return texto;
	}

	private String leer(String archivo) {
		ClassPathResource recurso = new ClassPathResource("prompts/" + archivo);
		try (InputStream entrada = recurso.getInputStream()) {
			return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException excepcion) {
			throw new IllegalStateException("No se encontró el prompt " + archivo);
		}
	}

}

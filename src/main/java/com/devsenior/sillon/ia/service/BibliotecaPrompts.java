package com.devsenior.sillon.ia.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Carga los prompts de src/main/resources/prompts/<nombre>.txt y reemplaza las variables {{variable}}.
 * Falla antes de llamar a la IA (sin gastar tokens) si el archivo no existe o queda una variable sin llenar.
 */
@Component
public class BibliotecaPrompts {

    public String cargar(String nombre) {
        return cargar(nombre, Map.of());
    }

    public String cargar(String nombre, Map<String, String> variables) {
        String plantilla = leer(nombre);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            plantilla = plantilla.replace("{{" + variable.getKey() + "}}",
                    variable.getValue() == null ? "" : variable.getValue());
        }
        if (plantilla.contains("{{")) {
            throw new IllegalStateException("El prompt '" + nombre + "' tiene variables sin reemplazar");
        }
        return plantilla;
    }

    private String leer(String nombre) {
        ClassPathResource recurso = new ClassPathResource("prompts/" + nombre + ".txt");
        if (!recurso.exists()) {
            throw new IllegalStateException("No existe el prompt '" + nombre + "'");
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el prompt '" + nombre + "'", ex);
        }
    }
}

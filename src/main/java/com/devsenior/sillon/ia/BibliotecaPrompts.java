package com.devsenior.sillon.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

    public String cargar(String nombre) {
        return cargar(nombre, Map.of());
    }

    public String cargar(String nombre, Map<String, String> variables) {
        String prompt = leerArchivo(nombre);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            prompt = prompt.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        if (prompt.contains("{{")) {
            throw new IllegalStateException("El prompt '" + nombre + "' tiene variables sin reemplazar");
        }
        return prompt;
    }

    private String leerArchivo(String nombre) {
        ClassPathResource archivo = new ClassPathResource("prompts/" + nombre + ".txt");
        try (InputStream entrada = archivo.getInputStream()) {
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No existe el prompt '" + nombre + "'", ex);
        }
    }
}
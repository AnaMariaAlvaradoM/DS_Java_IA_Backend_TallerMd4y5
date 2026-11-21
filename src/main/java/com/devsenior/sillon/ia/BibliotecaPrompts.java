package com.devsenior.sillon.ia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {
    public String cargar(String nombre, Map<String, String> variables) {
        try {
            ClassPathResource recurso = new ClassPathResource("prompts/" + nombre + ".txt");
            String prompt;
            try (var entrada = recurso.getInputStream()) {
                prompt = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
            }
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                prompt = prompt.replace("{{" + variable.getKey() + "}}", variable.getValue());
            }
            if (prompt.contains("{{")) {
                throw new IllegalStateException("El prompt tiene variables sin sustituir: " + nombre);
            }
            return prompt;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el prompt " + nombre, ex);
        }
    }
}

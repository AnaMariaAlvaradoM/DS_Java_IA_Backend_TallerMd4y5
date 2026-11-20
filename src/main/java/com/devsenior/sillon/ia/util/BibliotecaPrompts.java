package com.devsenior.sillon.ia.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class BibliotecaPrompts {

    public String cargar(String nombreArchivo, Map<String, String> variables) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + nombreArchivo);
            String plantilla = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                plantilla = plantilla.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return plantilla;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar el prompt: " + nombreArchivo, e);
        }
    }
}
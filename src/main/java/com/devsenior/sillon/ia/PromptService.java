package com.devsenior.sillon.ia;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class PromptService {

    public String cargarYRenderizar(String nombreArchivo, Map<String, String> variables) {
        String plantilla = cargarPlantilla(nombreArchivo);
        if (variables != null) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String valor = entry.getValue() != null ? entry.getValue() : "";
                plantilla = plantilla.replace("{{" + entry.getKey() + "}}", valor);
            }
        }
        return plantilla;
    }

    private String cargarPlantilla(String nombreArchivo) {
        ClassPathResource resource = new ClassPathResource("prompts/" + nombreArchivo);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el prompt del classpath: " + nombreArchivo, e);
            }
        }

        Path rutaLocal = Path.of("prompts", nombreArchivo);
        if (Files.exists(rutaLocal)) {
            try {
                return Files.readString(rutaLocal, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer el prompt local: " + nombreArchivo, e);
            }
        }

        throw new IllegalStateException("Archivo de prompt no encontrado: " + nombreArchivo);
    }
}

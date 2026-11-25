package com.devsenior.sillon.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.PasarelaException;

@Component
public class BibliotecaPrompts {

    public String obtenerPrompt(String nombre, Map<String, String> variables) {
        String plantilla = cargarPlantilla(nombre);
        if (variables == null || variables.isEmpty()) {
            return plantilla;
        }

        String resultado = plantilla;
        for (Map.Entry<String, String> entrada : variables.entrySet()) {
            String clave = "{" + entrada.getKey() + "}";
            String valor = entrada.getValue() != null ? entrada.getValue() : "";
            resultado = resultado.replace(clave, valor);
        }

        return resultado;
    }

    private String cargarPlantilla(String nombre) {
        String ruta = "prompts/" + (nombre.endsWith(".txt") ? nombre : nombre + ".txt");
        ClassPathResource recurso = new ClassPathResource(ruta);

        try (InputStream is = recurso.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PasarelaException("No se pudo cargar la plantilla de prompt: " + ruta);
        }
    }
}

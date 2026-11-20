
package com.devsenior.sillon.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

    private final ResourceLoader resourceLoader;

    public BibliotecaPrompts(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String cargar(
            String nombre,
            Map<String, String> variables
    ) {
        if (nombre == null
                || !nombre.matches("[a-zA-Z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "Nombre de prompt inválido"
            );
        }

        Resource recurso = resourceLoader.getResource(
                "classpath:prompts/" + nombre + ".txt"
        );

        String contenido;

        try (InputStream entrada = recurso.getInputStream()) {
            contenido = new String(
                    entrada.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "No fue posible cargar el prompt: " + nombre,
                    ex
            );
        }

        if (variables != null) {
            for (Map.Entry<String, String> variable
                    : variables.entrySet()) {

                String marcador = "{{"
                        + variable.getKey()
                        + "}}";

                String valor = variable.getValue() == null
                        ? ""
                        : variable.getValue();

                contenido = contenido.replace(
                        marcador,
                        valor
                );
            }
        }

        return contenido;
    }
}


package com.devsenior.sillon.ia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

    private static final String CARPETA = "prompts/";

    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[a-zA-Z0-9_-]+\\.txt");

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    public String cargar(String nombreArchivo) {

        if (nombreArchivo == null
                || !NOMBRE_VALIDO.matcher(nombreArchivo).matches()) {

            throw new IllegalArgumentException(
                    "Nombre de plantilla invalido");
        }

        ClassPathResource recurso = new ClassPathResource(CARPETA + nombreArchivo);

        if (!recurso.exists()) {
            throw new IllegalArgumentException(
                    "No existe la plantilla: " + nombreArchivo);
        }

        try (var entrada = recurso.getInputStream()) {

            return new String(
                    entrada.readAllBytes(),
                    StandardCharsets.UTF_8);

        } catch (IOException excepcion) {

            throw new IllegalStateException(
                    "No fue posible leer la plantilla: "
                            + nombreArchivo,
                    excepcion);
        }
    }

    public String procesar(
            String nombreArchivo,
            Map<String, String> variables) {

        if (variables == null) {
            throw new IllegalArgumentException(
                    "Las variables no pueden ser nulas");
        }

        String plantilla = cargar(nombreArchivo);

        Matcher coincidencias = VARIABLE.matcher(plantilla);

        StringBuilder resultado = new StringBuilder();

        while (coincidencias.find()) {

            String nombreVariable = coincidencias.group(1);

            if (!variables.containsKey(nombreVariable)
                    || variables.get(nombreVariable) == null) {

                throw new IllegalArgumentException(
                        "Falta la variable: " + nombreVariable);
            }

            coincidencias.appendReplacement(
                    resultado,
                    Matcher.quoteReplacement(
                            variables.get(nombreVariable)));
        }

        coincidencias.appendTail(resultado);

        return resultado.toString();
    }
}

package com.devsenior.sillon.ia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {

    private static final Pattern VARIABLES = Pattern.compile("\\{\\{(salon|cliente|datos|direccion|horario|catalogo)}}");

    public String recordatorio(String salon, String cliente, String datos) {
        return renderizar("recordatorio", Map.of("salon", salon, "cliente", cliente, "datos", datos));
    }

    public String chatbot(String salon, String direccion, String horario, String catalogo, String cliente) {
        return renderizar("chatbot", Map.of("salon", salon, "direccion", direccion, "horario", horario,
                "catalogo", catalogo, "cliente", cliente));
    }

    private String renderizar(String nombre, Map<String, String> variables) {
        String plantilla;
        try {
            plantilla = new ClassPathResource("prompts/" + nombre + ".txt").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo cargar el prompt " + nombre, ex);
        }
        return VARIABLES.matcher(plantilla).replaceAll(
                resultado -> Matcher.quoteReplacement(variables.get(resultado.group(1))));
    }
}

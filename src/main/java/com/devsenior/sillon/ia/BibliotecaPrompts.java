package com.devsenior.sillon.ia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Carga los prompts de src/main/resources/prompts y reemplaza las variables {{nombre}}. */
@Component
public class BibliotecaPrompts {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String cargar(String nombre, Map<String, String> variables) {
        String plantilla = cache.computeIfAbsent(nombre, BibliotecaPrompts::leer);
        Matcher m = VARIABLE.matcher(plantilla);
        StringBuilder resultado = new StringBuilder();
        while (m.find()) {
            String valor = variables.get(m.group(1));
            if (valor == null) {
                throw new IllegalStateException("Falta la variable '" + m.group(1) + "' del prompt " + nombre);
            }
            m.appendReplacement(resultado, Matcher.quoteReplacement(valor));
        }
        m.appendTail(resultado);
        return resultado.toString();
    }

    private static String leer(String nombre) {
        try (InputStream in = new ClassPathResource("prompts/" + nombre + ".txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No existe el prompt " + nombre, e);
        }
    }
}

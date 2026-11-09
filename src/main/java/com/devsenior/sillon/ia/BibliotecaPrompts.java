package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.NoEncontradoException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class BibliotecaPrompts {
    private static final Pattern NOMBRE = Pattern.compile("[a-zA-Z0-9_-]+\\.txt");
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{([a-zA-Z0-9_]+)\\}\\}");

    public String cargar(String archivo, Map<String, String> contexto) {
        if (archivo == null || !NOMBRE.matcher(archivo).matches())
            throw new NoEncontradoException("Prompt inexistente");
        ClassPathResource recurso = new ClassPathResource("prompts/" + archivo);
        if (!recurso.exists()) throw new NoEncontradoException("Prompt inexistente");
        try {
            String plantilla = recurso.getContentAsString(StandardCharsets.UTF_8);
            Matcher variables = VARIABLE.matcher(plantilla);
            StringBuilder resultado = new StringBuilder();
            while (variables.find()) {
                String valor = contexto.get(variables.group(1));
                if (valor == null) throw new IllegalArgumentException("Falta contexto para " + variables.group(1));
                variables.appendReplacement(resultado, Matcher.quoteReplacement(aislar(valor)));
            }
            variables.appendTail(resultado);
            return resultado.toString();
        } catch (IOException error) {
            throw new NoEncontradoException("Prompt inexistente");
        }
    }

    private String aislar(String valor) {
        return "<dato_no_confiable>\n" + valor.replace("<", "&lt;").replace(">", "&gt;")
                + "\n</dato_no_confiable>";
    }
}

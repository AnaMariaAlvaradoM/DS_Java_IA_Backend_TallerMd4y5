package com.devsenior.sillon.ia;

import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/** Secuencia reutilizable para las funcionalidades de las fases 8 y 9. */
@Service
public class FlujoIa {
    private final CreditosIaServicio creditos;
    private final BibliotecaPrompts prompts;
    private final AsistenteIa asistente;
    private final LectorJsonIa lector;

    public FlujoIa(CreditosIaServicio creditos, BibliotecaPrompts prompts,
                   AsistenteIa asistente, LectorJsonIa lector) {
        this.creditos = creditos;
        this.prompts = prompts;
        this.asistente = asistente;
        this.lector = lector;
    }

    public Resultado ejecutar(Runnable validarNegocio, Long salonId, String archivo, Map<String, String> contexto,
                              Map<String, Class<?>> campos) {
        validarNegocio.run();
        creditos.verificar(salonId);
        String prompt = prompts.cargar(archivo, contexto);
        RespuestaIa respuesta = asistente.responder(prompt);
        int restantes = creditos.consumir(salonId);
        JsonNode contenido = lector.leer(respuesta.contenido(), campos);
        return new Resultado(contenido, restantes, respuesta.factura());
    }

    public record Resultado(JsonNode contenido, int creditosRestantes, FacturaIa factura) { }
}

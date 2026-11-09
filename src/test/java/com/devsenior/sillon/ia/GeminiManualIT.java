package com.devsenior.sillon.ia;

import java.util.Map;
import org.springframework.boot.convert.DurationStyle;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba técnica optativa con la configuración local de secrets.properties.
 * No coincide con los patrones normales de Surefire (*Test, *Tests, *TestCase).
 * Ejecutar explícitamente: .\mvnw.cmd '-Dtest=GeminiManualIT' test
 * No crea ni consume créditos de un salón.
 */
class GeminiManualIT {
    @Test
    void respondeJsonYFacturaConConfiguracionReal() throws Exception {
        StandardEnvironment entorno = new StandardEnvironment();
        entorno.getPropertySources().addFirst(new ResourcePropertySource("secrets", "file:./secrets.properties"));
        entorno.getPropertySources().addLast(new ResourcePropertySource("application", "classpath:application.properties"));
        IaConfiguracion.Proveedor datos = new IaConfiguracion.Proveedor(
                entorno.getRequiredProperty("sillon.ia.openai.url"),
                entorno.getRequiredProperty("sillon.ia.openai.llave"),
                entorno.getRequiredProperty("sillon.ia.openai.modelo"), null, null);
        IaConfiguracion configuracion = new IaConfiguracion(datos, null,
                DurationStyle.detectAndParse(entorno.getRequiredProperty("sillon.ia.timeout")));
        ObjectMapper json = new ObjectMapper();
        ProveedorOpenAi gemini = new ProveedorOpenAi(
                new IaHttpConfiguracion().iaRestClientBuilder(configuracion), json, configuracion);
        LectorJsonIa lector = new LectorJsonIa(json);
        String llave = configuracion.openai().llave();
        assertTrue(llave != null && !llave.isBlank() && !"gemini_placeholder".equals(llave),
                "Configure sillon.ia.openai.llave en secrets.properties antes de la prueba manual");
        RespuestaIa respuesta;
        try {
            respuesta = gemini.generar("Responda únicamente un objeto JSON con la clave mensaje "
                    + "y el valor exacto hola.");
        } catch (FalloProveedorIa fallo) {
            String codigo = fallo.getCodigoHttp() == null ? "sin respuesta HTTP" : "HTTP " + fallo.getCodigoHttp();
            throw new AssertionError("Gemini falló: " + codigo + ", causa " + fallo.getTipo());
        }
        assertEquals("hola", lector.leer(respuesta.contenido(), Map.of("mensaje", String.class))
                .path("mensaje").asText());
        assertEquals("Gemini", respuesta.factura().proveedor());
        assertEquals(configuracion.openai().modelo(), respuesta.factura().modelo());
        assertNotNull(respuesta.factura().tokensEntrada(), "Gemini no informó tokens de entrada");
        assertNotNull(respuesta.factura().tokensSalida(), "Gemini no informó tokens de salida");
        assertTrue(respuesta.factura().tokensEntrada() > 0);
        assertTrue(respuesta.factura().tokensSalida() > 0);
        assertTrue(respuesta.factura().milisegundos() >= 0);
        System.out.println("Gemini respondió JSON válido; modelo=" + respuesta.factura().modelo()
                + ", tokensEntrada=" + respuesta.factura().tokensEntrada()
                + ", tokensSalida=" + respuesta.factura().tokensSalida());
    }
}

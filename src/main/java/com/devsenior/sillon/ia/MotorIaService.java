package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.ProveedorIaNoDisponibleException;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class MotorIaService {

    private final List<ProveedorIa> proveedores;
    private final ObjectMapper objectMapper;

    public MotorIaService(List<ProveedorIa> proveedores, ObjectMapper objectMapper) {
        this.proveedores = proveedores;
        this.objectMapper = objectMapper;
    }

    public RespuestaIa ejecutarConFallback(String promptSistema, String promptUsuario) {
        if (proveedores == null || proveedores.isEmpty()) {
            throw new ProveedorIaNoDisponibleException("No hay proveedores de IA configurados");
        }

        for (ProveedorIa proveedor : proveedores) {
            try {
                RespuestaIa respuesta = proveedor.generar(promptSistema, promptUsuario);
                if (respuesta != null && respuesta.contenido() != null && !respuesta.contenido().isBlank()) {
                    return respuesta;
                }
            } catch (Exception ignored) {
                // Intentar con el siguiente proveedor en la cadena de fallback
            }
        }

        throw new ProveedorIaNoDisponibleException("Ningún proveedor de IA está disponible en este momento");
    }

    public JsonNode extraerYValidarJson(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new PasarelaException("La respuesta de la IA llegó vacía");
        }

        String limpio = limpiarBloquesMarkdown(texto);
        try {
            JsonNode node = objectMapper.readTree(limpio);
            if (node == null || !node.isObject()) {
                throw new PasarelaException("La respuesta de la IA no es un objeto JSON válido");
            }
            return node;
        } catch (PasarelaException e) {
            throw e;
        } catch (Exception e) {
            throw new PasarelaException("La respuesta de la IA no es un JSON válido");
        }
    }

    private String limpiarBloquesMarkdown(String texto) {
        String limpio = texto.trim();
        if (limpio.startsWith("```")) {
            int primerSalto = limpio.indexOf('\n');
            if (primerSalto != -1) {
                limpio = limpio.substring(primerSalto + 1);
            } else {
                limpio = limpio.replaceFirst("^```[a-zA-Z]*", "");
            }
            if (limpio.endsWith("```")) {
                limpio = limpio.substring(0, limpio.length() - 3);
            }
        }
        return limpio.trim();
    }
}

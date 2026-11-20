package com.devsenior.sillon.ia.proveedor;

import com.devsenior.sillon.ia.dto.RespuestaIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ProveedorClaude implements ProveedorIa {

    private static final Logger log = LoggerFactory.getLogger(ProveedorClaude.class);

    private final String llaveClaude;

    public ProveedorClaude(@Value("${sillon.ia.claude.llave:}") String llaveClaude) {
        this.llaveClaude = llaveClaude;
    }

    @Override
    public String nombre() {
        return "claude-respaldo";
    }

    @Override
    public RespuestaIa generar(String instruccionesSistema, String mensajeUsuario) {
        if (llaveClaude == null || llaveClaude.isBlank()) {
            log.error("Fallo en proveedor de respaldo {}: no hay llave configurada", nombre());
            throw new IllegalStateException("Proveedor de respaldo sin llave configurada");
        }
        log.error("Fallo en proveedor de respaldo {}: no disponible en este entorno", nombre());
        throw new IllegalStateException("Proveedor de respaldo no disponible");
    }
}


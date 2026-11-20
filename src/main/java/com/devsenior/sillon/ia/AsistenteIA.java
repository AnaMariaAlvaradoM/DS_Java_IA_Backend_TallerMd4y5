
package com.devsenior.sillon.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AsistenteIA {

    private static final Logger log =
            LoggerFactory.getLogger(AsistenteIA.class);

    private final ProveedorIA principal;
    private final ProveedorIA respaldo;

    public AsistenteIA(
            ProveedorFormatoOpenAI principal,
            ProveedorClaude respaldo
    ) {
        this.principal = principal;
        this.respaldo = respaldo;
    }

    public ProveedorIA.Resultado generar(
            String instrucciones,
            String mensaje
    ) {

        try {
            log.info(
                    "Consultando proveedor principal: {}",
                    principal.nombre()
            );

            return principal.generar(
                    instrucciones,
                    mensaje
            );

        } catch (RuntimeException errorPrincipal) {

            log.error(
                    "Falló el proveedor principal {}: {}",
                    principal.nombre(),
                    errorPrincipal.getMessage()
            );
        }

        try {
            log.info(
                    "Intentando proveedor de respaldo: {}",
                    respaldo.nombre()
            );

            return respaldo.generar(
                    instrucciones,
                    mensaje
            );

        } catch (RuntimeException errorRespaldo) {

            log.error(
                    "Falló el proveedor de respaldo {}: {}",
                    respaldo.nombre(),
                    errorRespaldo.getMessage()
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Los proveedores de inteligencia artificial no están disponibles",
                    errorRespaldo
            );
        }
    }
}

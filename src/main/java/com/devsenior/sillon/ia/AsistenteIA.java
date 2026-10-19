package com.devsenior.sillon.ia;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsistenteIA {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIA.class);

    private final List<ProveedorIA> proveedores;

    public AsistenteIA(List<ProveedorIA> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIA completar(String instrucciones, String mensaje) {
        for (ProveedorIA proveedor : proveedores) {
            try {
                RespuestaIA respuesta = proveedor.completar(instrucciones, mensaje);
                log.info("IA ok proveedor={} modelo={} tokens={}/{} costoUsd={} ms={}",
                        respuesta.proveedor(), respuesta.modelo(),
                        respuesta.tokensEntrada(), respuesta.tokensSalida(),
                        respuesta.costoEstimadoUsd(), respuesta.milisegundos());
                return respuesta;
            } catch (ProveedorIAException ex) {
                if (ex.isErrorDeConfiguracion()) {
                    log.error("IA falló por configuración, se intenta el siguiente. {}", ex.getMessage());
                } else {
                    log.warn("IA falló temporalmente, se intenta el siguiente. {}", ex.getMessage());
                }
            }
        }
        throw new IaNoDisponibleException();
    }
}

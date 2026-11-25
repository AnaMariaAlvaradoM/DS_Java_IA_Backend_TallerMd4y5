package com.devsenior.sillon.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.IaNoDisponibleException;

@Service
public class AsistenteIa {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIa.class);

    private final List<ProveedorIa> proveedores;

    public AsistenteIa(List<ProveedorIa> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIa completar(String instrucciones, String mensaje) {
        if (proveedores == null || proveedores.isEmpty()) {
            throw new IaNoDisponibleException("No hay proveedores de IA configurados.");
        }

        for (ProveedorIa proveedor : proveedores) {
            try {
                RespuestaIa respuesta = proveedor.completar(instrucciones, mensaje);
                log.info("IA ok proveedor={} modelo={} tokens={}/{} costoUsd={} ms={}",
                        respuesta.factura().proveedor(),
                        respuesta.factura().modelo(),
                        respuesta.factura().tokensEntrada(),
                        respuesta.factura().tokensSalida(),
                        respuesta.factura().costoEstimadoUsd(),
                        respuesta.factura().milisegundos());
                return respuesta;
            } catch (ProveedorIaException ex) {
                if (ex.isErrorDeConfiguracion()) {
                    log.error("IA falló por configuración, se intenta el siguiente proveedor. Motivo: {}", ex.getMessage());
                } else {
                    log.warn("IA falló temporalmente, se intenta el siguiente proveedor. Motivo: {}", ex.getMessage());
                }
            } catch (Exception ex) {
                log.warn("IA falló con error inesperado en proveedor={}, se intenta el siguiente. Error: {}",
                        proveedor.nombre(), ex.getMessage());
            }
        }

        throw new IaNoDisponibleException("Ningún proveedor de IA está disponible en este momento.");
    }
}

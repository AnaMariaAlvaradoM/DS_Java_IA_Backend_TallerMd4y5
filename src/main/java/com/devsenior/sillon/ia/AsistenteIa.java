package com.devsenior.sillon.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.IaNoDisponibleException;

/** Fallback: prueba los proveedores en orden (Gemini y luego Claude). Si ninguno responde: 503. */
@Service
public class AsistenteIa {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIa.class);

    private final List<ProveedorIa> proveedores;

    public AsistenteIa(List<ProveedorIa> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        for (ProveedorIa proveedor : proveedores) {
            try {
                return proveedor.generar(instrucciones, mensajeUsuario);
            } catch (ErrorProveedorIa e) {
                if (e.isDeConfiguracion()) {
                    log.error("Proveedor de IA {} con error de configuración: {}", proveedor.nombre(), e.getMessage());
                } else {
                    log.warn("Proveedor de IA {} no disponible por ahora: {}", proveedor.nombre(), e.getMessage());
                }
            }
        }
        throw new IaNoDisponibleException("Ningún proveedor de IA está disponible en este momento");
    }
}

package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.ProveedorIaNoDisponibleException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsistenteIa {
    private static final Logger LOG = LoggerFactory.getLogger(AsistenteIa.class);
    private final List<ProveedorIa> proveedores;

    public AsistenteIa(List<ProveedorIa> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIa generar(String instrucciones, String mensajeUsuario) {
        for (ProveedorIa proveedor : proveedores) {
            try {
                return proveedor.generar(instrucciones, mensajeUsuario);
            } catch (RuntimeException ex) {
                LOG.warn("Proveedor de IA {} no disponible: {}", proveedor.nombre(), ex.toString());
            }
        }
        throw new ProveedorIaNoDisponibleException("Ningún proveedor de IA está disponible");
    }
}

package com.devsenior.sillon.ia;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsistenteIa {

    private static final Logger LOG = LoggerFactory.getLogger(AsistenteIa.class);
    private final List<ProveedorIa> proveedores;

    public AsistenteIa(List<ProveedorIa> proveedores) {
        this.proveedores = List.copyOf(proveedores);
    }

    public RespuestaIa responder(String instrucciones) {
        return consultar(proveedor -> proveedor.responder(instrucciones));
    }

    public RespuestaIa responder(String instrucciones, String mensaje) {
        return consultar(proveedor -> proveedor.responder(instrucciones, mensaje));
    }

    private RespuestaIa consultar(Function<ProveedorIa, RespuestaIa> llamada) {
        for (ProveedorIa proveedor : proveedores) {
            try {
                return llamada.apply(proveedor);
            } catch (ProveedorNoDisponibleException ex) {
                if (ex.esConfiguracion()) {
                    LOG.error("Proveedor {} no disponible: revisar configuracion", proveedor.nombre());
                } else {
                    LOG.warn("Proveedor {} no disponible temporalmente", proveedor.nombre());
                }
            }
        }
        throw new IaNoDisponibleException();
    }
}

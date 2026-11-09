package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.ProveedorNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AsistenteIa {
    private static final Logger LOG = LoggerFactory.getLogger(AsistenteIa.class);
    private final ProveedorIa principal;
    private final ProveedorIa respaldo;

    public AsistenteIa(@Qualifier("proveedorOpenAi") ProveedorIa principal,
                       @Qualifier("proveedorClaude") ProveedorIa respaldo) {
        this.principal = principal;
        this.respaldo = respaldo;
    }

    public RespuestaIa responder(String prompt) {
        return responderCon(proveedor -> proveedor.generar(prompt));
    }

    public RespuestaIa responder(PeticionIa peticion) {
        return responderCon(proveedor -> proveedor.generar(peticion));
    }

    private RespuestaIa responderCon(java.util.function.Function<ProveedorIa, RespuestaIa> invocacion) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("La llamada IA no puede ejecutarse en una transacción JPA");
        try {
            return invocacion.apply(principal);
        } catch (FalloProveedorIa fallo) {
            registrar(principal, fallo);
        }
        try {
            return invocacion.apply(respaldo);
        } catch (FalloProveedorIa fallo) {
            registrar(respaldo, fallo);
            throw new ProveedorNoDisponibleException("Ningún proveedor de IA está disponible");
        }
    }

    private void registrar(ProveedorIa proveedor, FalloProveedorIa fallo) {
        if (fallo.getTipo() == FalloProveedorIa.Tipo.CONFIGURACION
                || fallo.getTipo() == FalloProveedorIa.Tipo.AUTENTICACION)
            LOG.error("Proveedor IA {} falló: {}", proveedor.nombre(), fallo.getTipo());
        else LOG.warn("Proveedor IA {} falló: {}", proveedor.nombre(), fallo.getTipo());
    }
}

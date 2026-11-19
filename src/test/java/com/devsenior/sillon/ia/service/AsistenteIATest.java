package com.devsenior.sillon.ia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.ia.excepcion.IaNoDisponibleException;
import com.devsenior.sillon.ia.excepcion.ProveedorIAException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsistenteIATest {

    private ProveedorIA principal;
    private ProveedorIA respaldo;
    private AsistenteIA asistente;

    @BeforeEach
    void setUp() {
        principal = mock(ProveedorIA.class);
        respaldo = mock(ProveedorIA.class);
        asistente = new AsistenteIA(List.of(principal, respaldo));
    }

    @Test
    void completar_principalResponde_noLlamaAlRespaldo() {
        when(principal.completar("instrucciones", "mensaje")).thenReturn(respuesta("openai"));

        assertEquals("openai", asistente.completar("instrucciones", "mensaje").proveedor());
        verify(respaldo, never()).completar("instrucciones", "mensaje");
    }

    @Test
    void completar_principalFallaTemporalmente_usaElRespaldo() {
        when(principal.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIAException.desdeCodigoHttp("openai", 429));
        when(respaldo.completar("instrucciones", "mensaje")).thenReturn(respuesta("claude"));

        assertEquals("claude", asistente.completar("instrucciones", "mensaje").proveedor());
    }

    @Test
    void completar_principalConLlaveMala_tambienIntentaElRespaldo() {
        when(principal.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIAException.desdeCodigoHttp("openai", 400));
        when(respaldo.completar("instrucciones", "mensaje")).thenReturn(respuesta("claude"));

        assertEquals("claude", asistente.completar("instrucciones", "mensaje").proveedor());
    }

    @Test
    void completar_todosFallan_lanza503() {
        when(principal.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIAException.desdeCodigoHttp("openai", 400));
        when(respaldo.completar("instrucciones", "mensaje"))
                .thenThrow(new ProveedorIAException("claude", "sin conexión o tiempo agotado", false));

        assertThrows(IaNoDisponibleException.class, () -> asistente.completar("instrucciones", "mensaje"));
    }

    private RespuestaIA respuesta(String proveedor) {
        return new RespuestaIA("{}", proveedor, "modelo", 10, 5, BigDecimal.ZERO, 1);
    }
}

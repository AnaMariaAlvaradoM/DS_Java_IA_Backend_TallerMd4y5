package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsistenteIATest {

    @Mock
    private ProveedorIA principal;

    @Mock
    private ProveedorIA respaldo;

    private AsistenteIA asistenteIA;

    @BeforeEach
    void setUp() {
        asistenteIA = new AsistenteIA(List.of(principal, respaldo));
    }

    @Test
    void completar_principalResponde_noUsaRespaldo() {
        when(principal.completar(anyString(), anyString())).thenReturn(respuestaDe("openai"));

        RespuestaIA respuesta = asistenteIA.completar("instrucciones", "mensaje");

        assertEquals("openai", respuesta.proveedor());
        verify(respaldo, never()).completar(anyString(), anyString());
    }

    @Test
    void completar_principalFalla_usaRespaldo() {
        when(principal.completar(anyString(), anyString()))
                .thenThrow(ProveedorIAException.desdeEstadoHttp("openai", 429));
        when(respaldo.completar(anyString(), anyString())).thenReturn(respuestaDe("claude"));

        RespuestaIA respuesta = asistenteIA.completar("instrucciones", "mensaje");

        assertEquals("claude", respuesta.proveedor());
    }

    @Test
    void completar_todosFallan_lanzaIaNoDisponible() {
        when(principal.completar(anyString(), anyString()))
                .thenThrow(ProveedorIAException.desdeEstadoHttp("openai", 400));
        when(respaldo.completar(anyString(), anyString()))
                .thenThrow(new ProveedorIAException("claude", "sin conexión o tiempo agotado", false));

        assertThrows(IaNoDisponibleException.class,
                () -> asistenteIA.completar("instrucciones", "mensaje"));
    }

    private RespuestaIA respuestaDe(String proveedor) {
        return new RespuestaIA("MOLESTO", proveedor, "modelo-prueba", 120, 5,
                BigDecimal.ZERO, 300);
    }
}

package com.devsenior.sillon.ia;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.IaNoDisponibleException;

class AsistenteIaTest {

    @Test
    @DisplayName("Cuando el primer proveedor responde con éxito, no se invoca al segundo")
    void primerProveedorExitoso_noLlamaAlSegundo() {
        ProveedorIa p1 = mock(ProveedorIa.class);
        ProveedorIa p2 = mock(ProveedorIa.class);

        RespuestaIa respuestaEsperada = new RespuestaIa("Hola",
                new FacturaIa("openai", "modelo-1", 10, 5, BigDecimal.valueOf(0.001), 100));

        when(p1.completar("instrucciones", "mensaje")).thenReturn(respuestaEsperada);

        AsistenteIa asistente = new AsistenteIa(List.of(p1, p2));
        RespuestaIa respuesta = asistente.completar("instrucciones", "mensaje");

        assertNotNull(respuesta);
        assertEquals("Hola", respuesta.texto());
        verify(p2, never()).completar("instrucciones", "mensaje");
    }

    @Test
    @DisplayName("Cuando el primer proveedor falla temporalmente, se ejecuta el fallback hacia el segundo")
    void fallbackPorErrorTemporal() {
        ProveedorIa p1 = mock(ProveedorIa.class);
        ProveedorIa p2 = mock(ProveedorIa.class);

        when(p1.completar("instrucciones", "mensaje"))
                .thenThrow(new ProveedorIaException("openai", "503 servicio temporalmente caído", false));

        RespuestaIa respuestaP2 = new RespuestaIa("Respuesta de Claude",
                new FacturaIa("claude", "haiku", 12, 6, BigDecimal.valueOf(0.0005), 200));
        when(p2.completar("instrucciones", "mensaje")).thenReturn(respuestaP2);

        AsistenteIa asistente = new AsistenteIa(List.of(p1, p2));
        RespuestaIa respuesta = asistente.completar("instrucciones", "mensaje");

        assertNotNull(respuesta);
        assertEquals("Respuesta de Claude", respuesta.texto());
        assertEquals("claude", respuesta.factura().proveedor());
    }

    @Test
    @DisplayName("Cuando el primer proveedor falla por configuración (401), se ejecuta el fallback hacia el segundo")
    void fallbackPorErrorDeConfiguracion() {
        ProveedorIa p1 = mock(ProveedorIa.class);
        ProveedorIa p2 = mock(ProveedorIa.class);

        when(p1.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIaException.desdeEstadoHttp("openai", 401));

        RespuestaIa respuestaP2 = new RespuestaIa("Respuesta fallback",
                new FacturaIa("claude", "haiku", 8, 4, BigDecimal.valueOf(0.0003), 150));
        when(p2.completar("instrucciones", "mensaje")).thenReturn(respuestaP2);

        AsistenteIa asistente = new AsistenteIa(List.of(p1, p2));
        RespuestaIa respuesta = asistente.completar("instrucciones", "mensaje");

        assertNotNull(respuesta);
        assertEquals("Respuesta fallback", respuesta.texto());
    }

    @Test
    @DisplayName("Cuando todos los proveedores fallan, se lanza IaNoDisponibleException (503)")
    void todosLosProveedoresFallan_lanzaIaNoDisponible() {
        ProveedorIa p1 = mock(ProveedorIa.class);
        ProveedorIa p2 = mock(ProveedorIa.class);

        when(p1.completar("instrucciones", "mensaje"))
                .thenThrow(new ProveedorIaException("openai", "401", true));
        when(p2.completar("instrucciones", "mensaje"))
                .thenThrow(new ProveedorIaException("claude", "500", false));

        AsistenteIa asistente = new AsistenteIa(List.of(p1, p2));

        assertThrows(IaNoDisponibleException.class,
                () -> asistente.completar("instrucciones", "mensaje"));
    }
}

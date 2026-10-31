package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsistenteIATest {

    @Mock
    private ProveedorIA primero;
    @Mock
    private ProveedorIA segundo;

    private RespuestaIA respuestaDe(String proveedor) {
        return new RespuestaIA("ok", proveedor, "modelo", 1, 1, BigDecimal.ZERO, 10L);
    }

    @Test
    void siElPrimerProveedorRespondeBienNoSeLlamaAlSegundo() {
        when(primero.completar("instrucciones", "mensaje")).thenReturn(respuestaDe("primero"));
        AsistenteIA asistenteIA = new AsistenteIA(List.of(primero, segundo));

        RespuestaIA respuesta = asistenteIA.completar("instrucciones", "mensaje");

        assertThat(respuesta.proveedor()).isEqualTo("primero");
        verify(segundo, never()).completar(any(), any());
    }

    @Test
    void siElPrimerProveedorFallaPorConfiguracionSeUsaElSegundo() {
        when(primero.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIAException.desdeEstadoHttp("primero", 401));
        when(segundo.completar("instrucciones", "mensaje")).thenReturn(respuestaDe("segundo"));
        AsistenteIA asistenteIA = new AsistenteIA(List.of(primero, segundo));

        RespuestaIA respuesta = asistenteIA.completar("instrucciones", "mensaje");

        assertThat(respuesta.proveedor()).isEqualTo("segundo");
    }

    @Test
    void siAmbosProveedoresFallanDaIaNoDisponible() {
        when(primero.completar("instrucciones", "mensaje"))
                .thenThrow(ProveedorIAException.desdeEstadoHttp("primero", 401));
        when(segundo.completar("instrucciones", "mensaje"))
                .thenThrow(new ProveedorIAException("segundo", "sin conexion o tiempo agotado", false));
        AsistenteIA asistenteIA = new AsistenteIA(List.of(primero, segundo));

        assertThatThrownBy(() -> asistenteIA.completar("instrucciones", "mensaje"))
                .isInstanceOf(IaNoDisponibleException.class);
    }
}

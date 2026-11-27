package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.IaNoDisponibleException;
import com.devsenior.sillon.compartido.ProveedorIAException;
import com.devsenior.sillon.ia.proveedor.ProveedorIA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AsistenteIaTest {

    @Mock private ProveedorIA proveedorPrincipal;
    @Mock private ProveedorIA proveedorSecundario;

    @Test
    void usaProveedorSecundarioCuandoElPrincipalFallaConErrorRecuperable() {
        ProveedorIAException errorRecuperable =
                ProveedorIAException.desdeEstadoHttp("principal", 429);
        RespuestaIA respuestaEsperada = respuestaDe("secundario");
        when(proveedorPrincipal.completar("instrucciones", "mensaje"))
                .thenThrow(errorRecuperable);
        when(proveedorSecundario.completar("instrucciones", "mensaje"))
                .thenReturn(respuestaEsperada);

        AsistenteIA asistenteIA = new AsistenteIA(List.of(proveedorPrincipal, proveedorSecundario));

        assertEquals(respuestaEsperada, asistenteIA.completar("instrucciones", "mensaje"));
        verify(proveedorSecundario).completar("instrucciones", "mensaje");
    }

    @Test
    void errorNoRecuperableSePropagaSinIntentarProveedorSecundario() {
        ProveedorIAException errorNoRecuperable =
                ProveedorIAException.desdeEstadoHttp("principal", 401);
        when(proveedorPrincipal.completar("instrucciones", "mensaje"))
                .thenThrow(errorNoRecuperable);

        AsistenteIA asistenteIA = new AsistenteIA(List.of(proveedorPrincipal, proveedorSecundario));

        assertEquals(errorNoRecuperable,
                assertThrows(ProveedorIAException.class,
                        () -> asistenteIA.completar("instrucciones", "mensaje")));
        verify(proveedorSecundario, never()).completar("instrucciones", "mensaje");
    }

    @ParameterizedTest
    @CsvSource({
            "400, true",
            "401, true",
            "403, true",
            "404, true",
            "408, false",
            "429, false",
            "500, false",
            "502, false",
            "503, false"
    })
    void clasificaCodigosHttpComoConfiguracionONoRecuperables(int estado, boolean errorConfiguracion) {
        ProveedorIAException error = ProveedorIAException.desdeEstadoHttp("proveedor", estado);

        assertEquals(errorConfiguracion, error.isErrorDeConfiguracion());
    }

    @Test
    void clasificaErroresDeRedComoRecuperables() {
        ProveedorIAException errorRed = ProveedorIAException.desdeErrorDeRed("proveedor");

        assertFalse(errorRed.isErrorDeConfiguracion());
    }

    @Test
    void todosLosProveedoresNoDisponiblesLanzanExcepcionDeServicio() {
        doThrow(ProveedorIAException.desdeEstadoHttp("principal", 503))
                .when(proveedorPrincipal).completar("instrucciones", "mensaje");
        doThrow(ProveedorIAException.desdeErrorDeRed("secundario"))
                .when(proveedorSecundario).completar("instrucciones", "mensaje");
        AsistenteIA asistenteIA = new AsistenteIA(List.of(proveedorPrincipal, proveedorSecundario));

        assertThrows(IaNoDisponibleException.class,
                () -> asistenteIA.completar("instrucciones", "mensaje"));
    }

    private RespuestaIA respuestaDe(String proveedor) {
        return new RespuestaIA("respuesta", proveedor, "modelo-test", 1, 1, BigDecimal.ZERO, 1L);
    }
}


package com.devsenior.sillon.ia.proveedor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;

@ExtendWith(MockitoExtension.class)
class AsistenteIaTest {

    private static final String INSTRUCCIONES = "Responde exclusivamente en formato JSON";

    private static final String MENSAJE = "Cuanto cuesta el corte?";

    @Mock
    private ClienteOpenAiCompatibleIa gemini;

    @Mock
    private ClienteClaudeIa claude;

    private AsistenteIa asistenteIa;

    @BeforeEach
    void configurar() {

        asistenteIa = new AsistenteIa(gemini, claude);
    }

    @Test
    void utilizaGeminiCuandoEstaDisponible() {

        ResultadoProveedorIa resultado = resultadoDe("Gemini");

        when(gemini.generar(INSTRUCCIONES, MENSAJE))
                .thenReturn(resultado);

        ResultadoProveedorIa respuesta = asistenteIa.generar(
                INSTRUCCIONES,
                MENSAJE);

        assertSame(resultado, respuesta);

        assertEquals(
                "Gemini",
                respuesta.factura().proveedor());

        verify(gemini, times(1))
                .generar(INSTRUCCIONES, MENSAJE);

        verifyNoInteractions(claude);
    }

    @Test
    void utilizaClaudeCuandoGeminiNoEstaDisponible() {

        when(gemini.generar(INSTRUCCIONES, MENSAJE))
                .thenThrow(
                        new ExcepcionServicioNoDisponible(
                                "Gemini no disponible"));

        ResultadoProveedorIa resultado = resultadoDe("Claude");

        when(claude.generar(INSTRUCCIONES, MENSAJE))
                .thenReturn(resultado);

        ResultadoProveedorIa respuesta = asistenteIa.generar(
                INSTRUCCIONES,
                MENSAJE);

        assertSame(resultado, respuesta);

        assertEquals(
                "Claude",
                respuesta.factura().proveedor());

        verify(gemini, times(1))
                .generar(INSTRUCCIONES, MENSAJE);

        verify(claude, times(1))
                .generar(INSTRUCCIONES, MENSAJE);
    }

    @Test
    void devuelve503CuandoAmbosProveedoresFallan() {

        when(gemini.generar(INSTRUCCIONES, MENSAJE))
                .thenThrow(
                        new ExcepcionServicioNoDisponible(
                                "Gemini no disponible"));

        when(claude.generar(INSTRUCCIONES, MENSAJE))
                .thenThrow(
                        new ExcepcionServicioNoDisponible(
                                "Claude no disponible"));

        ExcepcionServicioNoDisponible excepcion = assertThrows(
                ExcepcionServicioNoDisponible.class,
                () -> asistenteIa.generar(
                        INSTRUCCIONES,
                        MENSAJE));

        assertTrue(
                excepcion.getMessage()
                        .contains("Ningun proveedor"));

        verify(gemini, times(1))
                .generar(INSTRUCCIONES, MENSAJE);

        verify(claude, times(1))
                .generar(INSTRUCCIONES, MENSAJE);
    }

    @Test
    void noOcultaJsonInvalidoDeGemini() {

        when(gemini.generar(INSTRUCCIONES, MENSAJE))
                .thenThrow(
                        new ExcepcionIntegracion(
                                "Gemini devolvio JSON invalido"));

        assertThrows(
                ExcepcionIntegracion.class,
                () -> asistenteIa.generar(
                        INSTRUCCIONES,
                        MENSAJE));

        verify(gemini, times(1))
                .generar(INSTRUCCIONES, MENSAJE);

        verifyNoInteractions(claude);
    }

    private ResultadoProveedorIa resultadoDe(
            String proveedor) {

        FacturaIa factura = new FacturaIa(
                proveedor,
                proveedor.equals("Gemini")
                        ? "gemini-2.5-flash"
                        : "claude-sonnet-4-5",
                100,
                30,
                BigDecimal.ZERO,
                120L);

        return new ResultadoProveedorIa(
                """
                        {"respuesta":"El corte cuesta 25000 COP.",
                         "escalar":false}
                        """,
                factura);
    }
}

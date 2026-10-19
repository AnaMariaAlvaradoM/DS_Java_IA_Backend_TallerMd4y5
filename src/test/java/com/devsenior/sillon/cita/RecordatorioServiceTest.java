package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class RecordatorioServiceTest {
    private final CitaService citas = mock(CitaService.class);
    private final CreditosService creditos = mock(CreditosService.class);
    private final BibliotecaPrompts prompts = spy(new BibliotecaPrompts());
    private final AsistenteIA ia = mock(AsistenteIA.class);
    private final LectorJsonIA lector = spy(new LectorJsonIA(JsonMapper.builder().build()));
    private final RecordatorioService servicio = new RecordatorioService(citas, creditos, prompts, ia, lector);

    @BeforeEach void preparar() {
        when(citas.datosRecordatorio(1L, 2L)).thenReturn(new DatosRecordatorio(2L, "Salón", "Andrés", "3102223344", "Estilista: Sara Mejía\nHora: 10:30 a. m."));
        when(creditos.consumir(1L)).thenReturn(9);
        when(ia.completar(anyString(), anyString())).thenReturn(respuesta("{\"mensaje\":\"Hola Andrés, Sara le espera a las 10:30.\"}"));
    }

    @Test void respuestaValidaRespetaOrdenYContrato() {
        RecordatorioResponse resultado = servicio.generar(1L, 2L);
        assertEquals(2L, resultado.citaId()); assertEquals("3102223344", resultado.telefonoCliente());
        assertEquals("Hola Andrés, Sara le espera a las 10:30.", resultado.mensaje()); assertEquals(9, resultado.creditosRestantes());
        assertEquals("openai", resultado.factura().proveedor()); assertEquals(10, resultado.factura().tokensEntrada());
        var orden = inOrder(citas, creditos, prompts, ia, lector);
        orden.verify(citas).datosRecordatorio(1L, 2L); orden.verify(creditos).verificar(1L);
        orden.verify(prompts).cargar(eq("recordatorio"), anyMap()); orden.verify(ia).completar(contains("\"mensaje\""), contains("Sara Mejía"));
        orden.verify(creditos).consumir(1L); orden.verify(lector).leer(any(), eq(RecordatorioService.BorradorIa.class));
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"mensaje\":null}", "{\"mensaje\":\" \"}", "{\"mensaje\":123}", "no es JSON"})
    void respuestaInvalidaEs502PeroConsumeCredito(String texto) {
        when(ia.completar(anyString(), anyString())).thenReturn(respuesta(texto));
        assertThrows(RespuestaIAInvalidaException.class, () -> servicio.generar(1L, 2L));
        verify(creditos).consumir(1L);
    }

    @Test void longitud700SeAceptaY701Es502ConCreditoConsumido() {
        when(ia.completar(anyString(), anyString())).thenReturn(respuesta("{\"mensaje\":\"" + "a".repeat(700) + "\"}"));
        assertEquals(700, servicio.generar(1L, 2L).mensaje().length());
        when(ia.completar(anyString(), anyString())).thenReturn(respuesta("{\"mensaje\":\"" + "a".repeat(701) + "\"}"));
        assertThrows(RespuestaIAInvalidaException.class, () -> servicio.generar(1L, 2L));
        verify(creditos, times(2)).consumir(1L);
    }

    @Test void citaInvalidaNoVerificaSaldoNiLlamaIa() {
        when(citas.datosRecordatorio(1L, 2L)).thenThrow(new ReglaNegocioException("Cita completada"));
        assertThrows(ReglaNegocioException.class, () -> servicio.generar(1L, 2L));
        verifyNoInteractions(creditos, prompts, ia, lector);
    }

    @Test void sinCreditosNoCargaPromptNiLlamaIa() {
        doThrow(new PagoRequeridoException("Sin créditos")).when(creditos).verificar(1L);
        assertThrows(PagoRequeridoException.class, () -> servicio.generar(1L, 2L));
        verifyNoInteractions(prompts, ia, lector); verify(creditos, never()).consumir(any());
    }

    @Test void proveedoresNoDisponiblesNoConsumenCredito() {
        when(ia.completar(anyString(), anyString())).thenThrow(new IaNoDisponibleException());
        assertThrows(IaNoDisponibleException.class, () -> servicio.generar(1L, 2L));
        verify(creditos, never()).consumir(any()); verifyNoInteractions(lector);
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "modelo-simulado", 10, 4, new BigDecimal("0.000013"), 20);
    }
}

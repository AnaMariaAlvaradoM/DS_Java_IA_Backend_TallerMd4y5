package com.devsenior.sillon.cita;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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

    @ParameterizedTest @ValueSource(strings = {"{}"})
    void respuestaInvalidaEs502PeroConsumeCredito(String texto) {
        when(ia.completar(anyString(), anyString())).thenReturn(respuesta(texto));
        var error = assertThrows(RespuestaIAInvalidaException.class, () -> servicio.generar(1L, 2L));
        assertEquals(org.springframework.http.HttpStatus.BAD_GATEWAY, error.getEstado());
        verify(creditos).consumir(1L);
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "modelo-simulado", 10, 4, new BigDecimal("0.000013"), 20);
    }
}

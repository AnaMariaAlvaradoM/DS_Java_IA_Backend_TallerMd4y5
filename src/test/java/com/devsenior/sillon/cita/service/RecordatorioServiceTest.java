package com.devsenior.sillon.cita.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.cita.dto.DatosRecordatorio;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.AsistenteIA;
import com.devsenior.sillon.ia.service.BibliotecaPrompts;
import com.devsenior.sillon.ia.service.LectorJsonIA;
import com.devsenior.sillon.salon.service.CreditosService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    private static final Long SALON_ID = 1L;
    private static final Long CITA_ID = 50L;

    @Mock
    private CitaService citaService;
    @Mock
    private CreditosService creditosService;
    @Mock
    private AsistenteIA asistenteIA;

    private RecordatorioService recordatorioService;

    @BeforeEach
    void setUp() {
        recordatorioService = new RecordatorioService(citaService, creditosService, new BibliotecaPrompts(),
                asistenteIA, new LectorJsonIA(JsonMapper.builder().build()));
    }

    @Test
    void generar_respuestaValida_devuelveMensajeTelefonoYCreditosRestantes() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datos());
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuesta("{\"mensaje\":\"Hola Andrés, le recordamos su cita con Sara Mejía a las 10:30 a. m.\"}"));
        when(creditosService.consumir(SALON_ID)).thenReturn(9);

        RecordatorioResponse respuesta = recordatorioService.generar(SALON_ID, CITA_ID);

        assertTrue(respuesta.mensaje().contains("Sara"));
        assertEquals("3102223344", respuesta.telefonoCliente());
        assertEquals(9, respuesta.creditosRestantes());
        assertEquals("openai", respuesta.factura().proveedor());
    }

    @Test
    void generar_elPromptLlevaLosDatosVerificadosYPideLaClaveMensaje() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datos());
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuesta("{\"mensaje\":\"Hola Andrés\"}"));
        when(creditosService.consumir(SALON_ID)).thenReturn(9);

        recordatorioService.generar(SALON_ID, CITA_ID);

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        verify(asistenteIA).completar(instrucciones.capture(), anyString());
        assertTrue(instrucciones.getValue().contains("- Estilista: Sara Mejía"));
        assertTrue(instrucciones.getValue().contains("- Hora: 10:30 a. m."));
        assertTrue(instrucciones.getValue().contains("mensaje"));
        assertFalse(instrucciones.getValue().contains("escalar"), "con 'escalar' la IA simulada respondería como chatbot");
    }

    @Test
    void generar_jsonSinMensaje_lanza502YaGastoElCredito() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datos());
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuesta("{\"texto\":\"Hola\"}"));
        when(creditosService.consumir(SALON_ID)).thenReturn(9);

        assertThrows(RespuestaIAInvalidaException.class, () -> recordatorioService.generar(SALON_ID, CITA_ID));
        verify(creditosService).consumir(SALON_ID);
    }

    @Test
    void generar_mensajeDeMasDe700Caracteres_lanza502() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datos());
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuesta("{\"mensaje\":\"" + "a".repeat(701) + "\"}"));
        when(creditosService.consumir(SALON_ID)).thenReturn(9);

        assertThrows(RespuestaIAInvalidaException.class, () -> recordatorioService.generar(SALON_ID, CITA_ID));
    }

    @Test
    void generar_citaCompletada_lanza409SinGastarCreditosNiLlamarALaIA() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenThrow(new ReglaNegocioException("COMPLETADA"));

        assertThrows(ReglaNegocioException.class, () -> recordatorioService.generar(SALON_ID, CITA_ID));
        verify(creditosService, never()).verificar(anyLong());
        verify(asistenteIA, never()).completar(anyString(), anyString());
    }

    @Test
    void generar_salonSinCreditos_lanza402SinLlamarALaIA() {
        when(citaService.datosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datos());
        doThrow(new PagoRequeridoException("sin créditos")).when(creditosService).verificar(SALON_ID);

        assertThrows(PagoRequeridoException.class, () -> recordatorioService.generar(SALON_ID, CITA_ID));
        verify(asistenteIA, never()).completar(anyString(), anyString());
        verify(creditosService, never()).consumir(anyLong());
    }

    private DatosRecordatorio datos() {
        String lineas = String.join("\n",
                "- Servicio: Corte clásico (45 minutos)",
                "- Estilista: Sara Mejía",
                "- Fecha: martes 20 de octubre",
                "- Hora: 10:30 a. m.",
                "- Valor: $35.000",
                "- Dirección: Calle 10 # 43-20, Medellín");
        return new DatosRecordatorio(CITA_ID, SALON_ID, "Barbería La Navaja", "Andrés Gómez", "3102223344", lineas);
    }

    private RespuestaIA respuesta(String texto) {
        return new RespuestaIA(texto, "openai", "gemini", 120, 40, BigDecimal.ZERO, 300);
    }
}

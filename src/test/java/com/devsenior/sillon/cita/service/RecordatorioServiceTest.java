package com.devsenior.sillon.cita.service;

import com.devsenior.sillon.cita.dto.DatosRecordatorio;
import com.devsenior.sillon.cita.dto.MensajeIa;
import com.devsenior.sillon.compartido.ServicioExternoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.service.CreditoIaService;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {
    private static final Long SALON_ID = 10L;
    private static final Long CITA_ID = 20L;

    @Mock private CitaService citaService;
    @Mock private CreditoIaService creditoIaService;
    @Mock private AsistenteIA asistenteIa;
    @Mock private LectorJsonIA lectorJson;
    @Mock private BibliotecaPrompts bibliotecaPrompts;

    private RecordatorioService recordatorioService;
    private DatosRecordatorio datos;
    private RespuestaIA respuestaIa;

    @BeforeEach
    void prepararServicio() {
        recordatorioService = new RecordatorioService(
                citaService, creditoIaService, asistenteIa, lectorJson, bibliotecaPrompts);
        datos = new DatosRecordatorio("Sillón Centro", "Laura", "+573001234567",
                "Servicio: Corte\nHora: 10:00");
        respuestaIa = new RespuestaIA(
                "{\"mensaje\":\"Hola Laura, confirme su asistencia.\"}",
                "mock", "modelo-test", 10, 12, BigDecimal.ZERO, 5L);
    }

    @Test
    void respuestaValidaRetornaMensajeYCreditosRestantes() {
        prepararLlamadaIA();
        when(creditoIaService.consumirCredito(SALON_ID)).thenReturn(7);
        when(lectorJson.leer(respuestaIa, MensajeIa.class))
                .thenReturn(new MensajeIa("Hola Laura, confirme su asistencia."));

        var resultado = recordatorioService.generarRecordatorio(SALON_ID, CITA_ID);

        assertEquals(CITA_ID, resultado.citaId());
        assertEquals("+573001234567", resultado.telefono());
        assertEquals("Hola Laura, confirme su asistencia.", resultado.mensaje());
        assertEquals(7, resultado.creditosRestantes());
        assertEquals(respuestaIa, resultado.factura());
        verify(creditoIaService).consumirCredito(SALON_ID);
    }

    @Test
    void jsonSinMensajeLanzaExcepcionDeServicioExterno() {
        prepararLlamadaIA();
        when(creditoIaService.consumirCredito(SALON_ID)).thenReturn(7);
        when(lectorJson.leer(respuestaIa, MensajeIa.class)).thenReturn(new MensajeIa(null));

        assertThrows(ServicioExternoException.class,
                () -> recordatorioService.generarRecordatorio(SALON_ID, CITA_ID));
    }

    private void prepararLlamadaIA() {
        when(citaService.obtenerDatosRecordatorio(CITA_ID)).thenReturn(datos);
        when(bibliotecaPrompts.cargar(eq("recordatorio"), any())).thenReturn("Prompt de prueba");
        when(asistenteIa.completar(any(String.class), any(String.class))).thenReturn(respuestaIa);
    }
}

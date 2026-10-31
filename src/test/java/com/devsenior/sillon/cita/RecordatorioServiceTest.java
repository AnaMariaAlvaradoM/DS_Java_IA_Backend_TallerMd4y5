package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    private static final Long SALON_ID = 1L;
    private static final Long CITA_ID = 10L;

    @Mock
    private CitaService citaService;
    @Mock
    private CreditosService creditosService;
    @Mock
    private AsistenteIA asistenteIA;
    @Mock
    private BibliotecaPrompts bibliotecaPrompts;
    @Mock
    private LectorJsonIA lectorJsonIA;

    private RecordatorioService recordatorioService;

    @BeforeEach
    void setUp() {
        recordatorioService = new RecordatorioService(citaService, creditosService, asistenteIA, bibliotecaPrompts,
                lectorJsonIA);
    }

    private DatosRecordatorio datosValidos() {
        return new DatosRecordatorio(CITA_ID, "Mi Salon", "Cliente Uno", "3001234567", "Servicio: Corte");
    }

    private RespuestaIA respuestaIaConTexto(String texto) {
        return new RespuestaIA(texto, "openai", "gemini-3.5-flash-lite", 10, 10, BigDecimal.ZERO, 100L);
    }

    @Test
    void unaRespuestaValidaDevuelveElMensajeYLosCreditosRestantes() {
        when(citaService.obtenerDatosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datosValidos());
        when(bibliotecaPrompts.cargar(eq("recordatorio"), any())).thenReturn("instrucciones");
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuestaIaConTexto("{\"mensaje\":\"Te esperamos manana a las 10am\"}"));
        when(lectorJsonIA.leer(any(), eq(RecordatorioIaRespuesta.class)))
                .thenReturn(new RecordatorioIaRespuesta("Te esperamos manana a las 10am"));
        when(creditosService.consumir(SALON_ID)).thenReturn(7);

        RecordatorioRespuesta respuesta = recordatorioService.generar(SALON_ID, CITA_ID);

        assertThat(respuesta.mensaje()).isEqualTo("Te esperamos manana a las 10am");
        assertThat(respuesta.creditosRestantes()).isEqualTo(7);
    }

    @Test
    void unJsonSinLaClaveMensajeDaRespuestaInvalida() {
        when(citaService.obtenerDatosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datosValidos());
        when(bibliotecaPrompts.cargar(eq("recordatorio"), any())).thenReturn("instrucciones");
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIaConTexto("{}"));
        when(lectorJsonIA.leer(any(), eq(RecordatorioIaRespuesta.class)))
                .thenReturn(new RecordatorioIaRespuesta(null));
        when(creditosService.consumir(SALON_ID)).thenReturn(6);

        assertThatThrownBy(() -> recordatorioService.generar(SALON_ID, CITA_ID))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    void unMensajeDeMasDe700CaracteresDaRespuestaInvalida() {
        String mensajeLargo = "a".repeat(701);
        when(citaService.obtenerDatosRecordatorio(SALON_ID, CITA_ID)).thenReturn(datosValidos());
        when(bibliotecaPrompts.cargar(eq("recordatorio"), any())).thenReturn("instrucciones");
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuestaIaConTexto("{\"mensaje\":\"" + mensajeLargo + "\"}"));
        when(lectorJsonIA.leer(any(), eq(RecordatorioIaRespuesta.class)))
                .thenReturn(new RecordatorioIaRespuesta(mensajeLargo));
        when(creditosService.consumir(SALON_ID)).thenReturn(6);

        assertThatThrownBy(() -> recordatorioService.generar(SALON_ID, CITA_ID))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    void unaCitaCompletadaDaConflictoYNoConsumeCreditos() {
        when(citaService.obtenerDatosRecordatorio(SALON_ID, CITA_ID))
                .thenThrow(new ReglaNegocioException(
                        "Solo se puede enviar recordatorio de citas pendientes o confirmadas"));

        assertThatThrownBy(() -> recordatorioService.generar(SALON_ID, CITA_ID))
                .isInstanceOf(ReglaNegocioException.class);

        verify(creditosService, never()).verificar(any());
        verify(creditosService, never()).consumir(any());
    }
}

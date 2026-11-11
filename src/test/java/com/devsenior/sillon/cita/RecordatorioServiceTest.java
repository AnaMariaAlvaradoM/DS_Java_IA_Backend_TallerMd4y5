package com.devsenior.sillon.cita;

import com.devsenior.sillon.chatbot.dto.RespuestaIaChatDto;
import com.devsenior.sillon.cita.dto.DatosRecordatorioCita;
import com.devsenior.sillon.cita.dto.MensajeIaDto;
import com.devsenior.sillon.cita.dto.RecordatorioRespuestaDto;
import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock
    private CitaService citaService;

    @Mock
    private CreditosService creditosService;

    @Mock
    private BibliotecaPrompts bibliotecaPrompts;

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private LectorJsonIA lectorJsonIA;

    @InjectMocks
    private RecordatorioService recordatorioService;

    private DatosRecordatorioCita datosCitaVerificados;

    @BeforeEach
    void setUp() {
        datosCitaVerificados = new DatosRecordatorioCita(
                "Carlos Pérez",
                "+573001234567",
                "2026-11-10 10:00",
                "Corte de cabello"
        );
    }

    private RespuestaIA crearRespuestaIAPrueba(String textoJson) {
        return new RespuestaIA("openai", "gpt-4o-mini", textoJson, 10, 5, BigDecimal.valueOf(0.001), 150L);
    }

    @Test
    @DisplayName("1. Debe generar recordatorio correctamente si los datos y créditos son válidos")
    void generarRecordatorioExitoso() {
        when(citaService.obtenerDatosVerificadosRecordatorio(10L, 1L)).thenReturn(datosCitaVerificados);
        when(bibliotecaPrompts.cargar(eq("recordatorio"), anyMap())).thenReturn("Prompt de prueba");

        RespuestaIA respuestaIA = crearRespuestaIAPrueba("{\"mensaje\":\"Hola Carlos, recordatorio de cita\"}");
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);

        // Usamos MensajeIaDto en lugar de RespuestaIaChatDto
        MensajeIaDto dtoIa = new MensajeIaDto("Hola Carlos, recordatorio de cita");
        when(lectorJsonIA.leer(respuestaIA, MensajeIaDto.class)).thenReturn(dtoIa);

        RecordatorioRespuestaDto resultado = recordatorioService.generarRecordatorio(10L, 1L);

        assertNotNull(resultado);
        verify(creditosService).verificar(1L);
        verify(creditosService).consumir(1L);
    }

    @Test
    @DisplayName("2. Debe propagar 404 (NoEncontradoException) si la cita no existe")
    void fallaCitaNoEncontrada() {
        when(citaService.obtenerDatosVerificadosRecordatorio(99L, 1L))
                .thenThrow(new NoEncontradoException("Cita no encontrada"));

        assertThrows(NoEncontradoException.class, () -> recordatorioService.generarRecordatorio(99L, 1L));

        verify(creditosService, never()).verificar(any());
        verify(asistenteIA, never()).completar(anyString(), anyString());
    }

    @Test
    @DisplayName("3. Debe propagar 404 si la cita no pertenece al salón")
    void fallaCitaDeOtroSalon() {
        when(citaService.obtenerDatosVerificadosRecordatorio(10L, 999L))
                .thenThrow(new NoEncontradoException("La cita no pertenece al salón"));

        assertThrows(NoEncontradoException.class, () -> recordatorioService.generarRecordatorio(10L, 999L));

        verify(creditosService, never()).verificar(any());
    }

    @Test
    @DisplayName("4. Debe propagar 409 (ConflictoExcepcion) si la cita está CANCELADA")
    void fallaCitaCancelada() {
        when(citaService.obtenerDatosVerificadosRecordatorio(10L, 1L))
                .thenThrow(new ConflictoExcepcion("La cita está cancelada"));

        assertThrows(ConflictoExcepcion.class, () -> recordatorioService.generarRecordatorio(10L, 1L));

        verify(creditosService, never()).verificar(any());
    }

    @Test
    @DisplayName("5. Debe lanzar ReglaNegocioException/PagoRequerido si no hay créditos suficientes (402)")
    void fallaSinCreditos() {
        when(citaService.obtenerDatosVerificadosRecordatorio(10L, 1L)).thenReturn(datosCitaVerificados);
        doThrow(new ReglaNegocioException("Sin créditos disponibles")).when(creditosService).verificar(1L);

        assertThrows(ReglaNegocioException.class, () -> recordatorioService.generarRecordatorio(10L, 1L));

        verify(asistenteIA, never()).completar(anyString(), anyString());
    }

    @Test
    @DisplayName("6. Debe lanzar 502 (RespuestaIAInvalidaException) si el JSON de la IA es nulo")
    void fallaRespuestaIAInvalida() {
        when(citaService.obtenerDatosVerificadosRecordatorio(10L, 1L)).thenReturn(datosCitaVerificados);
        when(bibliotecaPrompts.cargar(eq("recordatorio"), anyMap())).thenReturn("Prompt de prueba");

        RespuestaIA respuestaIA = crearRespuestaIAPrueba("texto invalido");
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, MensajeIaDto.class)).thenReturn(null);

        assertThrows(RespuestaIAInvalidaException.class, () -> recordatorioService.generarRecordatorio(10L, 1L));

        verify(creditosService).consumir(1L);
    }
}
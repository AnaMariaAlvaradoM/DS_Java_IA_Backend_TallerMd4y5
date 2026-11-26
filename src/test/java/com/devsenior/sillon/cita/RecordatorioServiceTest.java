package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditoService;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;

class RecordatorioServiceTest {

    private final CitaService citaService = mock(CitaService.class);
    private final CreditoService creditoService = mock(CreditoService.class);
    private final BibliotecaPrompts bibliotecaPrompts = mock(BibliotecaPrompts.class);
    private final AsistenteIa asistenteIa = mock(AsistenteIa.class);
    private RecordatorioService service;

    @BeforeEach
    void preparar() {
        service = new RecordatorioService(citaService, creditoService, bibliotecaPrompts, asistenteIa, new LectorJson());
        when(citaService.datosRecordatorio(1L, 5L)).thenReturn(new DatosRecordatorio(5L, "Barbería La Navaja",
                "Andrés Gómez", "3102223344", "Estilista: Sara Mejía\nHora: 10:30 a. m."));
        when(bibliotecaPrompts.cargar(eq("recordatorio"), anyMap())).thenReturn("instrucciones");
        when(creditoService.consumir(1L)).thenReturn(9);
    }

    @Test
    void respuestaValidaDevuelveElMensajeYLosCreditosRestantes() {
        when(asistenteIa.generar(anyString(), anyString())).thenReturn(new RespuestaIa(
                "{\"mensaje\":\"Hola Andrés, le recordamos su cita con Sara Mejía a las 10:30 a. m.\"}", "gemini",
                "gemini-2.5-flash", 200, 40, 500, 0.0002));

        RecordatorioRespuesta r = service.generar(1L, 5L);

        assertThat(r.mensaje()).contains("Sara", "10:30");
        assertThat(r.creditosRestantes()).isEqualTo(9);
        assertThat(r.telefonoCliente()).isEqualTo("3102223344");
        assertThat(r.factura().tokensEntrada()).isEqualTo(200);
    }

    @Test
    void jsonSinMensajeDa502() {
        when(asistenteIa.generar(anyString(), anyString()))
                .thenReturn(new RespuestaIa("{\"texto\":\"hola\"}", "gemini", "m", 10, 5, 100, 0));

        assertThatThrownBy(() -> service.generar(1L, 5L)).isInstanceOf(PasarelaException.class);
    }
}

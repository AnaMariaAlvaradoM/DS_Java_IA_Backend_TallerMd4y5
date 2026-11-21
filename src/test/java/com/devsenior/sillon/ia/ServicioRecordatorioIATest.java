
package com.devsenior.sillon.ia;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.DatosRecordatorio;
import com.devsenior.sillon.compartido.PagoRequeridoException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioRecordatorioIATest {

    @Test
    void sinCreditosNoDebeConsultarLaIA() {

        CitaService citas = mock(CitaService.class);
        ServicioCreditosIA creditos =
                mock(ServicioCreditosIA.class);

        BibliotecaPrompts prompts =
                mock(BibliotecaPrompts.class);

        AsistenteIA asistente = mock(AsistenteIA.class);
        LectorJsonIA lector = mock(LectorJsonIA.class);

        ServicioRecordatorioIA servicio =
                new ServicioRecordatorioIA(
                        citas,
                        creditos,
                        prompts,
                        asistente,
                        lector
                );

        when(citas.obtenerDatosRecordatorio(1L, 6L))
                .thenReturn(new DatosRecordatorio(
                        "Barbería La Navaja",
                        "Cliente de prueba",
                        "3110001002",
                        "Servicio: Corte clásico"
                ));

        doThrow(new PagoRequeridoException(
                "Sin créditos disponibles"
        )).when(creditos).verificar(1L);

        assertThrows(
                PagoRequeridoException.class,
                () -> servicio.generar(1L, 6L)
        );

        verifyNoInteractions(
                prompts,
                asistente,
                lector
        );

        verify(creditos, never())
                .consumir(anyLong());
    }
}

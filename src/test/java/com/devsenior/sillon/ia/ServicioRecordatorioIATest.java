
package com.devsenior.sillon.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cita.CitaService;
import com.devsenior.sillon.cita.DatosRecordatorio;
import com.devsenior.sillon.compartido.ErrorPasarelaException;
import com.devsenior.sillon.compartido.PagoRequeridoException;

import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServicioRecordatorioIATest {

    private CitaService citas;
    private ServicioCreditosIA creditos;
    private BibliotecaPrompts prompts;
    private AsistenteIA asistente;
    private ServicioRecordatorioIA servicio;

    private FacturaIA factura;

    @BeforeEach
    void configurar() {

        citas = mock(CitaService.class);
        creditos = mock(ServicioCreditosIA.class);
        prompts = mock(BibliotecaPrompts.class);
        asistente = mock(AsistenteIA.class);

        LectorJsonIA lector = new LectorJsonIA(
                JsonMapper.builder().build()
        );

        servicio = new ServicioRecordatorioIA(
                citas,
                creditos,
                prompts,
                asistente,
                lector
        );

        when(citas.obtenerDatosRecordatorio(1L, 6L))
                .thenReturn(new DatosRecordatorio(
                        "Barbería La Navaja",
                        "Carlos Pérez",
                        "3110001002",
                        """
                        Servicio: Corte clásico
                        Estilista: Sara Mejía
                        Fecha: 27 de noviembre de 2026
                        Hora: 10:30 a. m.
                        Valor: $40.000
                        Dirección: Calle 10 #20-30
                        """
                ));

        factura = new FacturaIA(
                "GEMINI",
                "gemini-simulado",
                120,
                60,
                0.0,
                25
        );
    }

    private void prepararPrompt() {

        when(prompts.cargar(
                eq("recordatorio"),
                anyMap()
        )).thenReturn("Instrucciones del recordatorio");
    }

    private void simularRespuestaIA(String contenido) {

        when(asistente.generar(
                anyString(),
                anyString()
        )).thenReturn(
                new ProveedorIA.Resultado(
                        contenido,
                        factura
                )
        );
    }

    // PRUEBA 1:
    // Sin créditos no debe consultar la IA.

    @Test
    void sinCreditosNoDebeConsultarLaIA() {

        doThrow(new PagoRequeridoException(
                "Sin créditos disponibles"
        )).when(creditos).verificar(1L);

        assertThrows(
                PagoRequeridoException.class,
                () -> servicio.generar(1L, 6L)
        );

        verifyNoInteractions(
                prompts,
                asistente
        );

        verify(creditos, never())
                .consumir(anyLong());
    }

    // PRUEBA 2:
    // Una respuesta válida devuelve
    // el mensaje y los créditos restantes.

    @Test
    void recordatorioValidoDevuelveMensajeYCreditos() {

        prepararPrompt();

        String mensaje =
                "Hola Carlos, le recordamos su cita "
                + "en Barbería La Navaja.";

        simularRespuestaIA(
                "{\"mensaje\":\"" + mensaje + "\"}"
        );

        when(creditos.consumir(1L))
                .thenReturn(9);

        RespuestaRecordatorioIA respuesta =
                servicio.generar(1L, 6L);

        assertNotNull(respuesta);

        assertEquals(
                6L,
                respuesta.citaId()
        );

        assertEquals(
                "3110001002",
                respuesta.telefonoCliente()
        );

        assertEquals(
                mensaje,
                respuesta.mensaje()
        );

        assertEquals(
                9,
                respuesta.creditosRestantes()
        );

        assertSame(
                factura,
                respuesta.factura()
        );

        verify(creditos, times(1))
                .verificar(1L);

        verify(creditos, times(1))
                .consumir(1L);
    }

    // PRUEBA 3:
    // Si la IA omite el campo mensaje,
    // debe producir un error de pasarela.

    @Test
    void jsonSinMensajeDebeResponderError502() {

        prepararPrompt();

        simularRespuestaIA("""
                {
                  "otro": "Respuesta sin mensaje"
                }
                """);

        when(creditos.consumir(1L))
                .thenReturn(9);

        ErrorPasarelaException error = assertThrows(
                ErrorPasarelaException.class,
                () -> servicio.generar(1L, 6L)
        );

        assertEquals(
                "La IA devolvió un recordatorio inválido",
                error.getMessage()
        );

        verify(asistente, times(1))
                .generar(anyString(), anyString());

        verify(creditos, times(1))
                .consumir(1L);
    }

    // PRUEBA 4:
    // Un mensaje de más de 700 caracteres
    // debe rechazarse.

    @Test
    void mensajeDeMasDe700CaracteresDebeRechazarse() {

        prepararPrompt();

        String mensajeLargo = "A".repeat(701);

        simularRespuestaIA(
                "{\"mensaje\":\"" + mensajeLargo + "\"}"
        );

        when(creditos.consumir(1L))
                .thenReturn(9);

        ErrorPasarelaException error = assertThrows(
                ErrorPasarelaException.class,
                () -> servicio.generar(1L, 6L)
        );

        assertEquals(
                "El recordatorio supera los 700 caracteres permitidos",
                error.getMessage()
        );

        verify(creditos, times(1))
                .consumir(1L);
    }
}

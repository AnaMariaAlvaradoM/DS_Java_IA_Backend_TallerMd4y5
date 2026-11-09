package com.devsenior.sillon.cita;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ProveedorNoDisponibleException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaServicio;
import com.devsenior.sillon.ia.FacturaIa;
import com.devsenior.sillon.ia.FlujoIa;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecordatorioServicioTests {
    private final CitaRepositorio repositorio = mock(CitaRepositorio.class);
    private final CreditosIaServicio creditos = mock(CreditosIaServicio.class);
    private final AsistenteIa asistente = mock(AsistenteIa.class);
    private final BibliotecaPrompts prompts = new BibliotecaPrompts();
    private final CitaServicio citas = new CitaServicio(repositorio, null, null, null, null,
            Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneId.of("America/Bogota")));
    private final RecordatorioServicio recordatorios = new RecordatorioServicio(citas,
            new FlujoIa(creditos, prompts, asistente, new LectorJsonIa(new ObjectMapper())));
    private Cita cita;

    @BeforeEach void preparar() {
        Salon salon = new Salon();
        salon.setId(7L); salon.setNombre("Barbería La Navaja");
        salon.setDireccion("Calle 10 # 43-20"); salon.setCiudad("Medellín");
        Cliente cliente = new Cliente();
        cliente.setNombre("Andrés Gómez"); cliente.setTelefono("3102223344");
        Servicio servicio = new Servicio();
        servicio.setNombre("Corte clásico"); servicio.setDuracionMinutos(45);
        Usuario estilista = new Usuario(); estilista.setNombre("Sara Mejía");
        cita = new Cita();
        cita.setId(11L); cita.setSalon(salon); cita.setCliente(cliente);
        cita.setServicio(servicio); cita.setEstilista(estilista);
        cita.setInicio(LocalDateTime.of(2026, 10, 20, 10, 30));
        cita.setPrecio(35000); cita.setEstado(EstadoCita.PENDIENTE);
        when(repositorio.findByIdAndSalon_Id(11L, 7L)).thenReturn(Optional.of(cita));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"PENDIENTE", "CONFIRMADA"})
    void recordatorioValidoConDatosYFactura(EstadoCita estado) {
        cita.setEstado(estado);
        FacturaIa factura = new FacturaIa("Gemini", "modelo-prueba", 18, 25, null, 33);
        when(asistente.responder(anyString())).thenReturn(new RespuestaIa(
                "{\"mensaje\":\"Sara le espera a las 10:30.\"}", factura));
        when(creditos.consumir(7L)).thenReturn(9);
        RecordatorioRespuesta respuesta = recordatorios.generar(11L, 7L);
        assertEquals(11L, respuesta.citaId());
        assertEquals("3102223344", respuesta.telefonoCliente());
        assertEquals(9, respuesta.creditosRestantes());
        assertTrue(respuesta.mensaje().contains("Sara"));
        assertTrue(respuesta.mensaje().contains("10:30"));
        assertEquals("Gemini", respuesta.factura().proveedor());
        assertEquals("modelo-prueba", respuesta.factura().modelo());
        assertEquals(18, respuesta.factura().tokensEntrada());
        assertEquals(25, respuesta.factura().tokensSalida());
        assertEquals(33, respuesta.factura().milisegundos());
        verify(creditos, times(1)).consumir(7L);
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(asistente).responder(captor.capture());
        String prompt = captor.getValue();
        for (String dato : new String[] {"Barbería La Navaja", "Andrés Gómez", "Corte clásico (45 minutos)",
                "Sara Mejía", "martes 20 de octubre", "10:30 a. m.", "$35.000", "Calle 10 # 43-20, Medellín"})
            assertTrue(prompt.contains(dato), dato);
        assertFalse(prompt.contains("\u00a0"));
        assertFalse(prompt.contains("\u202f"));
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCita.class, names = {"COMPLETADA", "CANCELADA", "NO_ASISTIO"})
    void estadoFinalDa409SinCreditoNiIa(EstadoCita estado) {
        cita.setEstado(estado);
        assertEquals(409, assertThrows(com.devsenior.sillon.compartido.ReglaNegocioException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verifyNoInteractions(creditos, asistente);
    }

    @Test void citaAjenaOInexistenteDa404() {
        assertEquals(404, assertThrows(NoEncontradoException.class,
                () -> recordatorios.generar(11L, 8L)).getEstado().value());
        verify(repositorio).findByIdAndSalon_Id(11L, 8L);
        verifyNoInteractions(creditos, asistente);
    }

    @Test void sinCreditosDa402SinLlamadaExterna() {
        doThrow(new PagoRequeridoException("Sin créditos")).when(creditos).verificar(7L);
        assertEquals(402, assertThrows(PagoRequeridoException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verifyNoInteractions(asistente);
        verify(creditos, never()).consumir(anyLong());
    }

    @Test void proveedoresFallidosDan503SinConsumo() {
        when(asistente.responder(anyString())).thenThrow(new ProveedorNoDisponibleException("Sin proveedor"));
        assertEquals(503, assertThrows(ProveedorNoDisponibleException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verify(creditos, never()).consumir(anyLong());
    }

    @Test void jsonSinMensajeDa502DespuesDelConsumo() {
        when(asistente.responder(anyString())).thenReturn(new RespuestaIa("{}", null));
        assertEquals(502, assertThrows(PasarelaException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verify(creditos).consumir(7L);
    }

    @Test void mensajeVacioDa502() {
        when(asistente.responder(anyString())).thenReturn(new RespuestaIa("{\"mensaje\":\"   \"}", null));
        assertEquals(502, assertThrows(PasarelaException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
    }

    @Test void mensajeQueNoEsTextoDa502() {
        when(asistente.responder(anyString())).thenReturn(new RespuestaIa("{\"mensaje\":123}", null));
        assertEquals(502, assertThrows(PasarelaException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verify(creditos).consumir(7L);
    }

    @Test void mensajeMuyLargoDa502() {
        when(asistente.responder(anyString())).thenReturn(new RespuestaIa(
                "{\"mensaje\":\"" + "x".repeat(701) + "\"}", null));
        assertEquals(502, assertThrows(PasarelaException.class,
                () -> recordatorios.generar(11L, 7L)).getEstado().value());
        verify(creditos).consumir(7L);
    }

    @Test void promptPideSoloClaveMensaje() {
        String prompt = prompts.cargar("recordatorio.txt",
                Map.of("salon", "Salon", "cliente", "Cliente", "datos", "Datos"));
        assertTrue(prompt.contains("\"mensaje\""));
        assertTrue(prompt.contains("SÍ"));
        assertTrue(prompt.contains("cuatro frases"));
    }
}

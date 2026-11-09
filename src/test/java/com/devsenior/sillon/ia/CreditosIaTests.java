package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.salon.SalonRepositorio;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditosIaTests {
    @Test void validaNegocioAntesDeConsultarCreditos() {
        CreditosIaServicio creditos = mock(CreditosIaServicio.class);
        BibliotecaPrompts prompts = mock(BibliotecaPrompts.class);
        AsistenteIa asistente = mock(AsistenteIa.class);
        FlujoIa flujo = new FlujoIa(creditos, prompts, asistente, mock(LectorJsonIa.class));
        assertThrows(IllegalArgumentException.class, () -> flujo.ejecutar(
                () -> { throw new IllegalArgumentException("Cita inválida"); },
                7L, "x.txt", Map.of(), Map.of()));
        verifyNoInteractions(creditos, prompts, asistente);
    }

    @Test void sinCreditosDa402YSinProveedor() {
        SalonRepositorio repo = mock(SalonRepositorio.class);
        when(repo.consultarCreditosIa(7L)).thenReturn(Optional.of(0));
        CreditosIaServicio creditos = new CreditosIaServicio(repo);
        BibliotecaPrompts prompts = mock(BibliotecaPrompts.class);
        AsistenteIa asistente = mock(AsistenteIa.class);
        FlujoIa flujo = new FlujoIa(creditos, prompts, asistente, mock(LectorJsonIa.class));
        assertEquals(402, assertThrows(PagoRequeridoException.class,
                () -> flujo.ejecutar(() -> { }, 7L, "x", Map.of(), Map.of())).getEstado().value());
        verifyNoInteractions(prompts, asistente);
    }

    @Test void consumeUnCreditoYDevuelveSaldo() {
        SalonRepositorio repo = mock(SalonRepositorio.class);
        when(repo.consumirCreditoIa(7L)).thenReturn(1);
        when(repo.consultarCreditosIa(7L)).thenReturn(Optional.of(4));
        assertEquals(4, new CreditosIaServicio(repo).consumir(7L));
        verify(repo, times(1)).consumirCreditoIa(7L);
        verify(repo, never()).save(any());
    }

    @Test void actualizacionCondicionalRechazaSegundoConsumoConcurrente() {
        SalonRepositorio repo = mock(SalonRepositorio.class);
        when(repo.consumirCreditoIa(7L)).thenReturn(1, 0);
        when(repo.consultarCreditosIa(7L)).thenReturn(Optional.of(0));
        CreditosIaServicio creditos = new CreditosIaServicio(repo);
        assertEquals(0, creditos.consumir(7L));
        assertThrows(PagoRequeridoException.class, () -> creditos.consumir(7L));
        verify(repo, times(2)).consumirCreditoIa(7L);
    }

    @Test void flujoConsumeAntesDeValidarJson() {
        CreditosIaServicio creditos = mock(CreditosIaServicio.class);
        BibliotecaPrompts prompts = mock(BibliotecaPrompts.class);
        AsistenteIa asistente = mock(AsistenteIa.class);
        LectorJsonIa lector = mock(LectorJsonIa.class);
        when(prompts.cargar("x.txt", Map.of())).thenReturn("prompt");
        when(asistente.responder("prompt")).thenReturn(new RespuestaIa("mal", null));
        when(lector.leer("mal", Map.of("mensaje", String.class))).thenThrow(new PasarelaException("JSON inválido"));
        assertThrows(PasarelaException.class, () -> new FlujoIa(creditos, prompts, asistente, lector)
                .ejecutar(() -> { }, 7L, "x.txt", Map.of(), Map.of("mensaje", String.class)));
        var orden = inOrder(creditos, prompts, asistente, lector);
        orden.verify(creditos).verificar(7L);
        orden.verify(prompts).cargar("x.txt", Map.of());
        orden.verify(asistente).responder("prompt");
        orden.verify(creditos).consumir(7L);
        orden.verify(lector).leer("mal", Map.of("mensaje", String.class));
    }
}

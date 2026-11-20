package com.devsenior.sillon.cita.modelo;

import com.devsenior.sillon.cliente.servicio.ClienteService;
import com.devsenior.sillon.salon.util.SlugUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoCitaTest {

    @Test
    void ocupaAgenda_soloPendienteYConfirmada() {
        assertTrue(EstadoCita.PENDIENTE.ocupaAgenda());
        assertTrue(EstadoCita.CONFIRMADA.ocupaAgenda());
        assertFalse(EstadoCita.COMPLETADA.ocupaAgenda());
        assertFalse(EstadoCita.CANCELADA.ocupaAgenda());
        assertFalse(EstadoCita.NO_ASISTIO.ocupaAgenda());
    }

    @Test
    void pendiente_puedePasarAConfirmadaYCancelada() {
        assertTrue(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.CONFIRMADA));
        assertTrue(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.CANCELADA));
        assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(EstadoCita.COMPLETADA));
    }

    @Test
    void confirmada_puedePasarACompletadaCanceladaONoAsistio() {
        assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.COMPLETADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.CANCELADA));
        assertTrue(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.NO_ASISTIO));
        assertFalse(EstadoCita.CONFIRMADA.puedeCambiarA(EstadoCita.PENDIENTE));
    }

    @Test
    void estadosTerminales_noPermitenTransiciones() {
        assertFalse(EstadoCita.COMPLETADA.puedeCambiarA(EstadoCita.CANCELADA));
        assertFalse(EstadoCita.CANCELADA.puedeCambiarA(EstadoCita.CONFIRMADA));
        assertFalse(EstadoCita.NO_ASISTIO.puedeCambiarA(EstadoCita.CONFIRMADA));
        assertFalse(EstadoCita.PENDIENTE.puedeCambiarA(null));
    }

    @Test
    void slugUtil_normalizaTildesYEspacios() {
        assertEquals("barberia-la-navaja", SlugUtil.generarSlug("Barbería La Navaja"));
    }

    @Test
    void slugUtil_quitaGuionesEnExtremos() {
        assertEquals("salon-elite", SlugUtil.generarSlug("  --Salón Élite!!  "));
    }

    @Test
    void normalizarTelefono_dejaSoloDigitosYMas() {
        assertEquals("+573001234567", ClienteService.normalizarTelefono("+57 (300) 123-4567"));
    }

    @Test
    void normalizarTelefono_nuloRetornaVacio() {
        assertEquals("", ClienteService.normalizarTelefono(null));
    }
}


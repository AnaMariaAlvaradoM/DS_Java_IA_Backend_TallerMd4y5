package com.devsenior.sillon.compartido;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cliente.ClienteService;
import com.devsenior.sillon.salon.GeneradorSlug;

class UtilidadesTest {

    @Test
    void elSlugQuitaTildesEspaciosYMayusculas() {
        assertThat(GeneradorSlug.normalizar("Barbería La Navaja")).isEqualTo("barberia-la-navaja");
        assertThat(GeneradorSlug.normalizar("  Salón Aurora!! ")).isEqualTo("salon-aurora");
    }

    @Test
    void elTelefonoSeGuardaSoloConDigitosYMas() {
        assertThat(ClienteService.normalizarTelefono("300 111 2233")).isEqualTo("3001112233");
        assertThat(ClienteService.normalizarTelefono("+57 (310) 222-3344")).isEqualTo("+573102223344");
    }
}

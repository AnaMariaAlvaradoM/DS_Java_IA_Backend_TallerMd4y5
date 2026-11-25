package com.devsenior.sillon.ia;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

class CreditosIaServicioTest {

    private final SalonRepository salonRepository = mock(SalonRepository.class);
    private final CreditosIaServicio creditosServicio = new CreditosIaServicio(salonRepository);

    @Test
    @DisplayName("Verificar créditos pasa cuando el salón tiene créditos disponibles")
    void verificarCreditos_conCreditos_pasa() {
        Salon salon = new Salon();
        salon.setCreditosIa(5);

        assertDoesNotThrow(() -> creditosServicio.verificarCreditos(salon));
    }

    @Test
    @DisplayName("Verificar créditos lanza PagoRequeridoException (402) cuando el salón tiene 0 créditos")
    void verificarCreditos_sinCreditos_lanza402() {
        Salon salon = new Salon();
        salon.setCreditosIa(0);

        assertThrows(PagoRequeridoException.class,
                () -> creditosServicio.verificarCreditos(salon));
    }

    @Test
    @DisplayName("Consumir crédito exitosamente descuenta 1 atómicamente y devuelve créditos restantes")
    void consumirCredito_exito() {
        when(salonRepository.descontarCredito(1L)).thenReturn(1);
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setCreditosIa(9);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        int restantes = creditosServicio.consumirCredito(1L);

        assertEquals(9, restantes);
        verify(salonRepository).descontarCredito(1L);
    }

    @Test
    @DisplayName("Consumir crédito cuando no quedan créditos (0 filas afectadas) lanza 402")
    void consumirCredito_sinSaldo_lanza402() {
        when(salonRepository.descontarCredito(1L)).thenReturn(0);

        assertThrows(PagoRequeridoException.class,
                () -> creditosServicio.consumirCredito(1L));
    }
}

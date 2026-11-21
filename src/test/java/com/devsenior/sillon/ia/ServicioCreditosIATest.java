
package com.devsenior.sillon.ia;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioCreditosIATest {

    private final SalonRepository salonRepository =
            mock(SalonRepository.class);

    private final ServicioCreditosIA servicio =
            new ServicioCreditosIA(salonRepository);

    private static final Long SALON_ID = 1L;

    @Test
    void verificarPermiteSalonConCreditos() {
        Salon salon = crearSalon(10);

        when(salonRepository.findById(SALON_ID))
                .thenReturn(Optional.of(salon));

        assertDoesNotThrow(() ->
                servicio.verificar(SALON_ID)
        );
    }

    @Test
    void verificarRechazaSalonSinCreditos() {
        Salon salon = crearSalon(0);

        when(salonRepository.findById(SALON_ID))
                .thenReturn(Optional.of(salon));

        assertThrows(PagoRequeridoException.class, () ->
                servicio.verificar(SALON_ID)
        );
    }

    @Test
    void consumirDevuelveSaldoRestante() {
        Salon salon = crearSalon(9);

        when(salonRepository.consumirCreditoIa(SALON_ID))
                .thenReturn(1);

        when(salonRepository.findById(SALON_ID))
                .thenReturn(Optional.of(salon));

        int restantes = servicio.consumir(SALON_ID);

        assertEquals(9, restantes);

        verify(salonRepository, times(1))
                .consumirCreditoIa(SALON_ID);
    }

    @Test
    void consumirRechazaSaldoAgotado() {
        when(salonRepository.consumirCreditoIa(SALON_ID))
                .thenReturn(0);

        when(salonRepository.existsById(SALON_ID))
                .thenReturn(true);

        assertThrows(PagoRequeridoException.class, () ->
                servicio.consumir(SALON_ID)
        );
    }

    @Test
    void verificarRechazaSalonInexistente() {
        when(salonRepository.findById(SALON_ID))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                servicio.verificar(SALON_ID)
        );
    }

    private Salon crearSalon(int creditos) {
        Salon salon = new Salon();
        salon.setId(SALON_ID);
        salon.setCreditosIa(creditos);
        return salon;
    }
}

package com.devsenior.sillon.salon.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditosServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private CreditosService creditosService;

    @Test
    void verificar_salonSinCreditos_lanza402() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonCon(0)));

        assertThrows(PagoRequeridoException.class, () -> creditosService.verificar(1L));
    }

    @Test
    void verificar_salonConCreditos_noLanza() {
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonCon(10)));

        assertDoesNotThrow(() -> creditosService.verificar(1L));
    }

    @Test
    void consumir_conCreditos_devuelveLosRestantes() {
        when(salonRepository.consumirCredito(1L)).thenReturn(1);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salonCon(9)));

        assertEquals(9, creditosService.consumir(1L));
    }

    @Test
    void consumir_otraPeticionGastoElUltimoCredito_lanza402() {
        when(salonRepository.consumirCredito(1L)).thenReturn(0);

        assertThrows(PagoRequeridoException.class, () -> creditosService.consumir(1L));
    }

    private Salon salonCon(int creditos) {
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setCreditosIa(creditos);
        return salon;
    }
}

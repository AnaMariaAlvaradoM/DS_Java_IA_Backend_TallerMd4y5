package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@ExtendWith(MockitoExtension.class)
class CreditosServiceTest {

    private static final Long SALON_ID = 1L;

    @Mock
    private SalonRepository salonRepository;

    private CreditosService creditosService;

    @BeforeEach
    void setUp() {
        creditosService = new CreditosService(salonRepository);
    }

    @Test
    void verificarConCeroCreditosDaPagoRequerido() {
        Salon salon = new Salon();
        salon.setId(SALON_ID);
        salon.setCreditosIa(0);
        when(salonRepository.findById(SALON_ID)).thenReturn(Optional.of(salon));

        assertThatThrownBy(() -> creditosService.verificar(SALON_ID)).isInstanceOf(PagoRequeridoException.class);
    }

    @Test
    void consumirSinCreditosDaPagoRequerido() {
        when(salonRepository.restarCredito(SALON_ID)).thenReturn(0);

        assertThatThrownBy(() -> creditosService.consumir(SALON_ID)).isInstanceOf(PagoRequeridoException.class);
    }
}

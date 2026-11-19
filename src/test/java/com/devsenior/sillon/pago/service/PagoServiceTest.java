package com.devsenior.sillon.pago.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.model.TipoCompra;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.SalonService;
import com.stripe.StripeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private StripeClient stripeClient;
    @Mock
    private SalonService salonService;

    @Test
    void crearSesion_suscripcionConSalonYaPro_lanza409SinLlamarAStripe() {
        StripeProperties stripe = new StripeProperties("http://localhost:12111", "sk_test_x", "whsec_x", "price_pro", "price_creditos");
        PagoService pagoService = new PagoService(stripeClient, stripe, salonService, "http://localhost:8080");
        Salon salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.PRO);
        when(salonService.obtenerEntidad(1L)).thenReturn(salon);

        assertThrows(ReglaNegocioException.class,
                () -> pagoService.crearSesion(1L, "camilo@navaja.co", TipoCompra.SUSCRIPCION_PRO));
        verifyNoInteractions(stripeClient);
    }
}

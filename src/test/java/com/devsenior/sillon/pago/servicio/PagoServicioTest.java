
package com.devsenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.pago.configuracion.PropiedadesStripe;
import com.devsenior.sillon.pago.dto.RespuestaSesionPago;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.checkout.SessionService;

class PagoServicioTest {

    private StripeClient stripeClient;
    private CheckoutService checkoutService;
    private SessionService sessionService;
    private SalonRepositorio salonRepositorio;
    private PagoServicio pagoServicio;
    private Salon salon;

    @BeforeEach
    void preparar() {

        stripeClient = mock(StripeClient.class);
        checkoutService = mock(CheckoutService.class);
        sessionService = mock(SessionService.class);
        salonRepositorio = mock(SalonRepositorio.class);

        PropiedadesStripe propiedades = new PropiedadesStripe(
                "sk_test_ejemplo",
                "whsec_ejemplo",
                "price_pro_ejemplo",
                "price_creditos_ejemplo",
                "https://api.stripe.com");

        pagoServicio = new PagoServicio(
                stripeClient,
                propiedades,
                salonRepositorio,
                "http://localhost:8080");

        salon = new Salon();
        salon.setId(1L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
    }

    private void prepararCheckout(String id, String url)
            throws Exception {

        Session sesion = mock(Session.class);

        when(sesion.getId()).thenReturn(id);
        when(sesion.getUrl()).thenReturn(url);

        when(stripeClient.checkout())
                .thenReturn(checkoutService);

        when(checkoutService.sessions())
                .thenReturn(sessionService);

        when(sessionService.create(any(SessionCreateParams.class)))
                .thenReturn(sesion);
    }

    @Test
    void crearSuscripcionProDevuelveSesionDeStripe()
            throws Exception {

        when(salonRepositorio.findById(1L))
                .thenReturn(Optional.of(salon));

        prepararCheckout(
                "cs_pro_123",
                "https://checkout.stripe.com/pro");

        RespuestaSesionPago respuesta =
                pagoServicio.crearSesion(
                        1L,
                        "dueno@ejemplo.com",
                        TipoCompra.SUSCRIPCION_PRO);

        assertEquals(
                TipoCompra.SUSCRIPCION_PRO,
                respuesta.tipo());

        assertEquals("cs_pro_123", respuesta.sesionId());
        assertEquals(
                "https://checkout.stripe.com/pro",
                respuesta.urlPago());

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(10, salon.getCreditosIa());

        verify(sessionService).create(
                any(SessionCreateParams.class));

        verify(salonRepositorio, never()).save(any());
    }

    @Test
    void comprarCreditosDevuelveSesionSinCambiarSaldo()
            throws Exception {

        when(salonRepositorio.findById(1L))
                .thenReturn(Optional.of(salon));

        prepararCheckout(
                "cs_creditos_123",
                "https://checkout.stripe.com/creditos");

        RespuestaSesionPago respuesta =
                pagoServicio.crearSesion(
                        1L,
                        "dueno@ejemplo.com",
                        TipoCompra.PAQUETE_CREDITOS);

        assertEquals(
                TipoCompra.PAQUETE_CREDITOS,
                respuesta.tipo());

        assertEquals(
                "cs_creditos_123",
                respuesta.sesionId());

        assertEquals(
                "https://checkout.stripe.com/creditos",
                respuesta.urlPago());

        assertEquals(Plan.GRATIS, salon.getPlan());
        assertEquals(10, salon.getCreditosIa());

        verify(sessionService).create(
                any(SessionCreateParams.class));

        verify(salonRepositorio, never()).save(any());
    }

    @Test
    void salonProNoPuedeCrearOtraSuscripcion() {

        salon.setPlan(Plan.PRO);

        when(salonRepositorio.findById(1L))
                .thenReturn(Optional.of(salon));

        assertThrows(
                ExcepcionConflicto.class,
                () -> pagoServicio.crearSesion(
                        1L,
                        "dueno@ejemplo.com",
                        TipoCompra.SUSCRIPCION_PRO));

        verifyNoInteractions(stripeClient);
        verify(salonRepositorio, never()).save(any());
    }

    @Test
    void salonInexistenteNoPuedeCrearSesion() {

        when(salonRepositorio.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> pagoServicio.crearSesion(
                        999L,
                        "dueno@ejemplo.com",
                        TipoCompra.PAQUETE_CREDITOS));

        verifyNoInteractions(stripeClient);
        verify(salonRepositorio, never()).save(any());
    }
}

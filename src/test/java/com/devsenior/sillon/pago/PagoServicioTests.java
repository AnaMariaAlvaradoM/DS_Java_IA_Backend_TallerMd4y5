package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepositorio;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.devsenior.sillon.usuario.UsuarioRepositorio;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagoServicioTests {
    private final SalonRepositorio salones = mock(SalonRepositorio.class);
    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final CheckoutGateway checkout = mock(CheckoutGateway.class);
    private final StripeConfiguracion configuracion = new StripeConfiguracion("sk_test_ficticia",
            "whsec_ficticio", "price_pro_ficticio", "price_creditos_ficticio", "https://api.stripe.com");
    private PagoServicio pagos;
    private Salon salon;
    private UsuarioPrincipal principal;

    @BeforeEach
    void preparar() {
        salon = new Salon();
        salon.setId(7L);
        salon.setPlan(Plan.GRATIS);
        Usuario dueno = new Usuario();
        dueno.setId(3L);
        dueno.setSalon(salon);
        dueno.setEmail("dueno@ejemplo.co");
        dueno.setRol(Rol.DUENO);
        dueno.setActivo(true);
        principal = new UsuarioPrincipal(dueno);
        when(salones.findById(7L)).thenReturn(Optional.of(salon));
        when(usuarios.findByIdAndSalon_IdAndRol(3L, 7L, Rol.DUENO)).thenReturn(Optional.of(dueno));
        pagos = new PagoServicio(salones, usuarios, checkout, configuracion, "https://sillon.example/");
    }

    @Test
    void checkoutProUsaPrecioModoMetadatosYUrlsDelBackend() {
        when(checkout.crear(any())).thenReturn(new SesionCheckout("cs_test_pro", "https://checkout.stripe.com/pro"));
        SesionPagoRespuesta respuesta = pagos.suscripcion(principal);
        assertEquals(TipoCompra.SUSCRIPCION_PRO, respuesta.tipo());
        assertEquals("cs_test_pro", respuesta.sesionId());
        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(checkout).crear(captor.capture());
        SessionCreateParams parametros = captor.getValue();
        assertEquals(SessionCreateParams.Mode.SUBSCRIPTION, parametros.getMode());
        assertEquals("dueno@ejemplo.co", parametros.getCustomerEmail());
        assertEquals("7", parametros.getClientReferenceId());
        assertEquals("7", parametros.getMetadata().get("salonId"));
        assertEquals("SUSCRIPCION_PRO", parametros.getMetadata().get("tipoCompra"));
        assertEquals("7", parametros.getSubscriptionData().getMetadata().get("salonId"));
        assertEquals("SUSCRIPCION_PRO", parametros.getSubscriptionData().getMetadata().get("tipoCompra"));
        assertEquals("price_pro_ficticio", parametros.getLineItems().get(0).getPrice());
        assertEquals("https://sillon.example/api/pagos/resultado?estado=exito", parametros.getSuccessUrl());
        assertEquals("https://sillon.example/api/pagos/resultado?estado=cancelado", parametros.getCancelUrl());
    }

    @Test
    void checkoutCreditosEsPagoUnicoYNoIncluyeDatosDeSuscripcion() {
        when(checkout.crear(any())).thenReturn(new SesionCheckout("cs_test_creditos", "https://checkout.stripe.com/creditos"));
        assertEquals(TipoCompra.PAQUETE_CREDITOS, pagos.creditos(principal).tipo());
        ArgumentCaptor<SessionCreateParams> captor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(checkout).crear(captor.capture());
        assertEquals(SessionCreateParams.Mode.PAYMENT, captor.getValue().getMode());
        assertEquals("price_creditos_ficticio", captor.getValue().getLineItems().get(0).getPrice());
        assertNull(captor.getValue().getSubscriptionData());
    }

    @Test
    void planProRechazaOtraSuscripcionAntesDeStripe() {
        salon.setPlan(Plan.PRO);
        assertThrows(ReglaNegocioException.class, () -> pagos.suscripcion(principal));
        verifyNoInteractions(checkout);
    }

    @Test
    void respuestaInutilizableDeStripeDa502() {
        when(checkout.crear(any())).thenReturn(new SesionCheckout("cs_test", null));
        assertThrows(PasarelaException.class, () -> pagos.creditos(principal));
    }
}

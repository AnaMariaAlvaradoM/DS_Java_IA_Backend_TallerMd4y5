package com.devsenior.sillon.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonResponse;
import com.devsenior.sillon.salon.SalonService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.devsenior.sillon.usuario.Usuario;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PagoServiceTest {
    private final SalonService salones = mock(SalonService.class);
    private final PasarelaStripe pasarela = mock(PasarelaStripe.class);
    private final StripeProperties properties = new StripeProperties();
    private PagoService pagos;
    private UsuarioPrincipal principal;

    @BeforeEach void preparar() {
        properties.setPrecioPro("price_pro_prueba"); properties.setPrecioCreditos("price_creditos_prueba");
        pagos = new PagoService(salones, pasarela, properties, "http://localhost:8080/");
        Salon salon = new Salon(); salon.setId(1L);
        Usuario usuario = new Usuario(); usuario.setId(2L); usuario.setSalon(salon);
        usuario.setEmail("dueno@ejemplo.test"); usuario.setRol(Rol.DUENO);
        principal = new UsuarioPrincipal(usuario);
        when(salones.consultar(1L)).thenReturn(respuesta(Plan.GRATIS));
        Session sesion = new Session(); sesion.setId("cs_test_prueba"); sesion.setUrl("https://checkout.stripe.test/pago");
        when(pasarela.crearSesion(any())).thenReturn(sesion);
    }

    @Test void proUsaPrecioDelBackendYMetadataEnSesionYSuscripcion() {
        PagoResponse pago = pagos.crear(principal, TipoCompra.SUSCRIPCION_PRO);
        ArgumentCaptor<SessionCreateParams> captura = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(pasarela).crearSesion(captura.capture());
        var parametros = captura.getValue();
        assertEquals(SessionCreateParams.Mode.SUBSCRIPTION, parametros.getMode());
        assertEquals("price_pro_prueba", parametros.getLineItems().get(0).getPrice());
        assertEquals(1L, parametros.getLineItems().get(0).getQuantity());
        assertEquals("1", parametros.getClientReferenceId());
        assertEquals("dueno@ejemplo.test", parametros.getCustomerEmail());
        assertEquals("SUSCRIPCION_PRO", parametros.getMetadata().get("tipoCompra"));
        assertEquals(parametros.getMetadata(), parametros.getSubscriptionData().getMetadata());
        assertEquals("http://localhost:8080/api/pagos/resultado?estado=exito", parametros.getSuccessUrl());
        assertEquals("http://localhost:8080/api/pagos/resultado?estado=cancelado", parametros.getCancelUrl());
        assertEquals("cs_test_prueba", pago.sesionId());
    }

    @Test void paqueteEsPagoUnicoConPrecioConfigurado() {
        pagos.crear(principal, TipoCompra.PAQUETE_CREDITOS);
        ArgumentCaptor<SessionCreateParams> captura = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(pasarela).crearSesion(captura.capture());
        assertEquals(SessionCreateParams.Mode.PAYMENT, captura.getValue().getMode());
        assertEquals("price_creditos_prueba", captura.getValue().getLineItems().get(0).getPrice());
    }

    @Test void yaProNoLlamaAStripe() {
        when(salones.consultar(1L)).thenReturn(respuesta(Plan.PRO));
        assertThrows(ReglaNegocioException.class, () -> pagos.crear(principal, TipoCompra.SUSCRIPCION_PRO));
        verifyNoInteractions(pasarela);
    }

    private SalonResponse respuesta(Plan plan) {
        return new SalonResponse(1L, "Salón", "salon", "Bogotá", "Calle 1", "3001112233", plan, 10, null, null);
    }
}

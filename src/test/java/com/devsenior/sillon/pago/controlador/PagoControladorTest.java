
package com.devsenior.sillon.pago.controlador;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.pago.dto.RespuestaSesionPago;
import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.servicio.PagoServicio;
import com.devsenior.sillon.pago.servicio.ProcesadorWebhookStripe;
import com.devsenior.sillon.pago.servicio.ValidadorWebhookStripe;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.model.Event;

class PagoControladorTest {

    private PagoServicio pagoServicio;
    private ValidadorWebhookStripe validadorWebhook;
    private ProcesadorWebhookStripe procesadorWebhook;
    private UsuarioPrincipal usuario;
    private PagoControlador controlador;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {

        pagoServicio = mock(PagoServicio.class);
        validadorWebhook = mock(ValidadorWebhookStripe.class);
        procesadorWebhook = mock(ProcesadorWebhookStripe.class);
        usuario = mock(UsuarioPrincipal.class);

        when(usuario.getSalonId()).thenReturn(1L);
        when(usuario.getUsername())
                .thenReturn("dueno@ejemplo.com");

        controlador = new PagoControlador(
                pagoServicio,
                validadorWebhook,
                procesadorWebhook);

        HandlerMethodArgumentResolver resolvedorUsuario =
                new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(
                            MethodParameter parametro) {

                        return parametro.hasParameterAnnotation(
                                AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parametro,
                            ModelAndViewContainer contenedor,
                            NativeWebRequest solicitud,
                            org.springframework.web.bind.support.WebDataBinderFactory fabrica) {

                        return usuario;
                    }
                };

        mvc = MockMvcBuilders
                .standaloneSetup(controlador)
                .setCustomArgumentResolvers(resolvedorUsuario)
                .build();
    }

    @Test
    void suscripcionDevuelve200YCamposOficiales()
            throws Exception {

        when(pagoServicio.crearSesion(
                1L,
                "dueno@ejemplo.com",
                TipoCompra.SUSCRIPCION_PRO))
                .thenReturn(new RespuestaSesionPago(
                        TipoCompra.SUSCRIPCION_PRO,
                        "cs_pro_123",
                        "https://checkout.stripe.com/pro"));

        mvc.perform(post("/api/pagos/suscripcion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo")
                        .value("SUSCRIPCION_PRO"))
                .andExpect(jsonPath("$.sesionId")
                        .value("cs_pro_123"))
                .andExpect(jsonPath("$.urlPago")
                        .value("https://checkout.stripe.com/pro"));

        verify(pagoServicio).crearSesion(
                1L,
                "dueno@ejemplo.com",
                TipoCompra.SUSCRIPCION_PRO);
    }

    @Test
    void creditosDevuelve200YCamposOficiales()
            throws Exception {

        when(pagoServicio.crearSesion(
                1L,
                "dueno@ejemplo.com",
                TipoCompra.PAQUETE_CREDITOS))
                .thenReturn(new RespuestaSesionPago(
                        TipoCompra.PAQUETE_CREDITOS,
                        "cs_creditos_123",
                        "https://checkout.stripe.com/creditos"));

        mvc.perform(post("/api/pagos/creditos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo")
                        .value("PAQUETE_CREDITOS"))
                .andExpect(jsonPath("$.sesionId")
                        .value("cs_creditos_123"))
                .andExpect(jsonPath("$.urlPago")
                        .value("https://checkout.stripe.com/creditos"));

        verify(pagoServicio).crearSesion(
                1L,
                "dueno@ejemplo.com",
                TipoCompra.PAQUETE_CREDITOS);
    }

    @Test
    void webhookValidoDevuelve200YProcesado()
            throws Exception {

        String cuerpo = "{\"id\":\"evt_123\"}";
        String firma = "firma_de_prueba";

        Event evento = mock(Event.class);

        when(validadorWebhook.validar(cuerpo, firma))
                .thenReturn(evento);

        when(procesadorWebhook.procesar(evento))
                .thenReturn(new RespuestaWebhook(true, true));

        mvc.perform(post("/api/pagos/webhook")
                        .header("Stripe-Signature", firma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recibido")
                        .value(true))
                .andExpect(jsonPath("$.procesado")
                        .value(true));

        verify(validadorWebhook).validar(cuerpo, firma);
        verify(procesadorWebhook).procesar(evento);
    }

    @Test
    void resultadoExitosoDevuelve200ConMensaje()
            throws Exception {

        mvc.perform(get("/api/pagos/resultado")
                        .param("estado", "exito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("El proceso de pago ha finalizado."));

        verifyNoInteractions(pagoServicio);
    }

    @Test
    void resultadoCanceladoDevuelve200ConMensaje()
            throws Exception {

        mvc.perform(get("/api/pagos/resultado")
                        .param("estado", "cancelado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje")
                        .value("El proceso de pago fue cancelado."));

        verifyNoInteractions(pagoServicio);
    }

    @Test
    void webhookInvalidoNoLlegaAlProcesador() {

        String cuerpo = "{\"id\":\"evt_invalido\"}";

        when(validadorWebhook.validar(cuerpo, null))
                .thenThrow(new ExcepcionSolicitudInvalida(
                        "El webhook requiere una firma valida"));

        assertThrows(
                ExcepcionSolicitudInvalida.class,
                () -> controlador.webhook(cuerpo, null));

        verifyNoInteractions(procesadorWebhook);
    }
}

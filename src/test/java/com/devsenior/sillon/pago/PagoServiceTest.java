package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaDeNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.Rol;
import com.stripe.StripeClient;
import com.stripe.net.HttpClient;
import com.stripe.net.HttpHeaders;
import com.stripe.net.StripeRequest;
import com.stripe.net.StripeResponse;

class PagoServiceTest {

    private final HttpClient transporte = mock(HttpClient.class);
    private final SalonRepository salones = mock(SalonRepository.class);
    private final UsuarioPrincipal dueno = new UsuarioPrincipal(1L, 7L, "dueno@prueba.example",
            "no-usada", Rol.DUENO, true);
    private final Salon salon = new Salon();
    private PagoService pagos;

    @BeforeEach
    void preparar() {
        salon.setId(7L);
        salon.setPlan(Plan.GRATIS);
        salon.setCreditosIa(10);
        given(salones.findById(7L)).willReturn(Optional.of(salon));
        StripeClient cliente = StripeClient.builder().setApiKey("sk_test_simulada")
                .setApiBase("http://127.0.0.1:12345").setHttpClient(transporte).build();
        pagos = new PagoService(salones, cliente,
                new ConfiguracionStripe("sk_test_simulada", "price_pro_servidor", "price_creditos_servidor",
                        "http://127.0.0.1:12345"),
                "https://sillon.example/");
    }

    @ParameterizedTest
    @EnumSource(TipoCompra.class)
    void enviaPrecioModoUrlsYMetadataDelServidorSinCambiarElSalon(TipoCompra tipo) throws Exception {
        responder(200, "{\"id\":\"cs_test_simulada\",\"object\":\"checkout.session\","
                + "\"url\":\"https://checkout.stripe.com/prueba\",\"livemode\":false}");

        var resultado = pagos.crearSesion(dueno, tipo);

        assertThat(resultado.tipo()).isEqualTo(tipo);
        assertThat(resultado.sesionId()).isEqualTo("cs_test_simulada");
        var solicitud = ArgumentCaptor.forClass(StripeRequest.class);
        verify(transporte).requestWithRetries(solicitud.capture());
        assertThat(solicitud.getValue().url().toString()).isEqualTo("http://127.0.0.1:12345/v1/checkout/sessions");
        Map<String, Object> parametros = solicitud.getValue().params();
        boolean pro = tipo == TipoCompra.SUSCRIPCION_PRO;
        Map<String, String> metadata = Map.of("salonId", "7", "tipo", tipo.name());
        assertThat(parametros)
                .containsEntry("mode", pro ? "subscription" : "payment")
                .containsEntry("customer_email", dueno.getUsername())
                .containsEntry("client_reference_id", "7")
                .containsEntry("metadata", metadata)
                .containsEntry("success_url", "https://sillon.example/api/pagos/resultado?estado=exito")
                .containsEntry("cancel_url", "https://sillon.example/api/pagos/resultado?estado=cancelado")
                .containsEntry("line_items", List.of(Map.of("price",
                        pro ? "price_pro_servidor" : "price_creditos_servidor", "quantity", 1L)));
        if (pro) {
            assertThat(parametros).containsEntry("subscription_data", Map.of("metadata", metadata));
        } else {
            assertThat(parametros).doesNotContainKey("subscription_data");
        }
        assertThat(salon.getPlan()).isEqualTo(Plan.GRATIS);
        assertThat(salon.getCreditosIa()).isEqualTo(10);
    }

    @Test
    void rechazaProAntesDeInvocarStripe() {
        salon.setPlan(Plan.PRO);
        assertThatThrownBy(() -> pagos.crearSesion(dueno, TipoCompra.SUSCRIPCION_PRO))
                .isInstanceOf(ReglaDeNegocioException.class);
        verifyNoInteractions(transporte);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null",
            "no-es-json",
            "{}",
            "{\"id\":\"cs_test_simulada\"}",
            "{\"id\":\"\",\"url\":\"https://checkout.stripe.com/prueba\"}",
            "{\"id\":\"cs_test_simulada\",\"url\":\"no-es-url\"}",
            "{\"id\":\"cs_test_simulada\",\"url\":\"https://checkout.stripe.com/prueba\",\"livemode\":true}"
    })
    void rechazaSesionesIncompletasOInvalidas(String respuesta) throws Exception {
        responder(200, respuesta);
        assertThatThrownBy(() -> pagos.crearSesion(dueno, TipoCompra.PAQUETE_CREDITOS))
                .isInstanceOf(ErrorDePasarelaException.class);
    }

    @Test
    void traduceErroresDelProveedorSinExponerSuRespuesta() throws Exception {
        responder(400, "{\"error\":{\"type\":\"invalid_request_error\",\"message\":\"detalle-privado\"}}");
        assertThatThrownBy(() -> pagos.crearSesion(dueno, TipoCompra.PAQUETE_CREDITOS))
                .isInstanceOf(ErrorDePasarelaException.class)
                .hasMessageNotContaining("detalle-privado");
    }

    @Test
    void noPermiteModoRealEnElClienteDeEsteBloque() {
        var configuracion = new ConfiguracionStripe("clave-no-de-prueba", "price_pro", "price_creditos",
                "http://127.0.0.1:12345");
        assertThatThrownBy(() -> new ClienteStripeConfiguracion().stripeClient(configuracion))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(configuracion.claveSecreta());
        assertThat(configuracion.toString()).doesNotContain(configuracion.claveSecreta());
    }

    @Test
    void salonInexistenteNoContactaStripe() {
        given(salones.findById(7L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> pagos.crearSesion(dueno, TipoCompra.PAQUETE_CREDITOS))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(transporte);
    }

    @Test
    void salonProPuedeComprarCreditosSinCambiarSuPlan() throws Exception {
        salon.setPlan(Plan.PRO);
        responder(200, "{\"id\":\"cs_test_simulada\",\"url\":\"https://checkout.stripe.com/prueba\",\"livemode\":false}");
        assertThat(pagos.crearSesion(dueno, TipoCompra.PAQUETE_CREDITOS).tipo())
                .isEqualTo(TipoCompra.PAQUETE_CREDITOS);
        assertThat(salon.getPlan()).isEqualTo(Plan.PRO);
        assertThat(salon.getCreditosIa()).isEqualTo(10);
    }

    private void responder(int estado, String cuerpo) throws Exception {
        given(transporte.requestWithRetries(any())).willReturn(new StripeResponse(estado, HttpHeaders.of(Map.of()), cuerpo));
    }
}

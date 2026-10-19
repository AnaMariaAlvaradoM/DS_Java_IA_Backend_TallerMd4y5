package com.devsenior.sillon.pago;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.stripe.param.checkout.SessionCreateParams;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PasarelaStripeTest {
    private HttpServer servidor;
    private final AtomicReference<String> cuerpo = new AtomicReference<>();
    private final AtomicReference<String> respuesta = new AtomicReference<>(
            "{\"id\":\"cs_test_simulada\",\"object\":\"checkout.session\",\"url\":\"https://checkout.stripe.test/pago\"}");
    private PasarelaStripe pasarela;

    @BeforeEach void preparar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/v1/checkout/sessions", intercambio -> {
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] datos = respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, datos.length);
            intercambio.getResponseBody().write(datos); intercambio.close();
        });
        servidor.start();
        StripeProperties properties = new StripeProperties();
        properties.setClaveSecreta("sk_test_simulador_local");
        properties.setUrlApi("http://127.0.0.1:" + servidor.getAddress().getPort());
        pasarela = new PasarelaStripe(new ConfiguracionStripe().stripeClient(properties), properties);
    }
    @AfterEach void cerrar() { servidor.stop(0); }

    @Test void clienteUsaLaDireccionConfigurableYElEndpointOficial() {
        var sesion = pasarela.crearSesion(SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT).build());
        assertEquals("cs_test_simulada", sesion.getId());
        assertEquals("mode=payment", cuerpo.get());
    }
    @Test void respuestaSinDatosValidosEs502() {
        respuesta.set("{\"object\":\"checkout.session\"}");
        assertThrows(PasarelaException.class,
                () -> pasarela.crearSesion(SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT).build()));
    }
}

package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
public class PasarelaStripe {
    private final StripeClient client;
    private final StripeProperties properties;

    public PasarelaStripe(StripeClient client, StripeProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public Session crearSesion(SessionCreateParams parametros) {
        if (!properties.getClaveSecreta().startsWith("sk_test_")) {
            throw new PasarelaException("Stripe debe estar configurado en modo prueba");
        }
        try {
            Session sesion = client.v1().checkout().sessions().create(parametros);
            if (sesion.getId() == null || sesion.getId().isBlank() || sesion.getUrl() == null
                    || !sesion.getUrl().startsWith("https://")) {
                throw new PasarelaException("Stripe no devolvió una sesión de pago válida");
            }
            return sesion;
        } catch (StripeException ex) {
            throw new PasarelaException("No fue posible crear la sesión de pago en Stripe");
        }
    }
}

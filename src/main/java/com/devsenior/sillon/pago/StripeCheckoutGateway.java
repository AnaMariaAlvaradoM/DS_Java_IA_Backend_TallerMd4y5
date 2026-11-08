package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.PasarelaException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

@Component
public class StripeCheckoutGateway implements CheckoutGateway {
    private final StripeClient cliente;

    public StripeCheckoutGateway(StripeClient cliente) { this.cliente = cliente; }

    @Override
    public SesionCheckout crear(SessionCreateParams parametros) {
        try {
            Session sesion = cliente.v1().checkout().sessions().create(parametros);
            return new SesionCheckout(sesion.getId(), sesion.getUrl());
        } catch (StripeException excepcion) {
            throw new PasarelaException("No se pudo crear la sesión de pago");
        }
    }
}

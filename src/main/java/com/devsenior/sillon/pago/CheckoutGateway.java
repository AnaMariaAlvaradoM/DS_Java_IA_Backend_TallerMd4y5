package com.devsenior.sillon.pago;

import com.stripe.param.checkout.SessionCreateParams;

public interface CheckoutGateway {
    SesionCheckout crear(SessionCreateParams parametros);
}

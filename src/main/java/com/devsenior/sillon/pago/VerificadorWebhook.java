package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import java.time.Clock;
import org.springframework.stereotype.Component;

@Component
public class VerificadorWebhook {
    private final StripeProperties properties;
    private final Clock reloj;
    public VerificadorWebhook(StripeProperties properties, Clock reloj) { this.properties = properties; this.reloj = reloj; }

    public Event verificar(String cuerpo, String firma) {
        if (firma == null || firma.isBlank()) { throw new FirmaInvalidaException("Falta la firma de Stripe"); }
        try {
            Event evento = Webhook.constructEvent(cuerpo, firma, properties.getSecretoWebhook(), Webhook.DEFAULT_TOLERANCE, reloj);
            if (evento.getId() == null || evento.getType() == null || evento.getData() == null) {
                throw new FirmaInvalidaException("El evento de Stripe no es válido");
            }
            return evento;
        } catch (SignatureVerificationException | RuntimeException ex) {
            throw new FirmaInvalidaException("La firma o el evento de Stripe no es válido");
        }
    }
}

package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookVerificador implements VerificadorWebhook {
    private final String secretoWebhook;

    public StripeWebhookVerificador(StripeConfiguracion configuracion) {
        this.secretoWebhook = configuracion.secretoWebhook();
    }

    @Override
    public EventoVerificado verificar(String cuerpo, String firma) {
        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException("Falta la firma de Stripe");
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(cuerpo, firma, secretoWebhook);
        } catch (SignatureVerificationException | IllegalArgumentException excepcion) {
            throw new FirmaInvalidaException("Firma de Stripe inválida");
        }
        if (evento.getId() == null || evento.getId().isBlank()
                || evento.getType() == null || evento.getType().isBlank()) {
            throw new PasarelaException("El evento de Stripe no contiene identificador o tipo");
        }
        if ("checkout.session.completed".equals(evento.getType())) {
            StripeObject objeto = objeto(evento);
            if (!(objeto instanceof Session sesion)) {
                throw new PasarelaException("Stripe no devolvió una sesión válida en el evento");
            }
            if (!"paid".equals(sesion.getPaymentStatus())) {
                return new EventoVerificado(evento.getId(), evento.getType(), null, null, false, null);
            }
            Map<String, String> metadata = sesion.getMetadata();
            if (metadata == null) { throw new PasarelaException("Faltan metadatos de la sesión"); }
            try {
                Long salonId = Long.valueOf(metadata.get("salonId"));
                TipoCompra tipo = TipoCompra.valueOf(metadata.get("tipoCompra"));
                if (salonId <= 0) { throw new IllegalArgumentException(); }
                return new EventoVerificado(evento.getId(), evento.getType(), salonId, tipo,
                        true, sesion.getSubscription());
            } catch (IllegalArgumentException | NullPointerException excepcion) {
                throw new PasarelaException("Metadatos de pago inválidos");
            }
        }
        if ("customer.subscription.deleted".equals(evento.getType())) {
            StripeObject objeto = objeto(evento);
            if (!(objeto instanceof Subscription suscripcion) || suscripcion.getId() == null) {
                throw new PasarelaException("Stripe no devolvió una suscripción válida en el evento");
            }
            return new EventoVerificado(evento.getId(), evento.getType(), null, null,
                    false, suscripcion.getId());
        }
        return new EventoVerificado(evento.getId(), evento.getType(), null, null, false, null);
    }

    private StripeObject objeto(Event evento) {
        var deserializador = evento.getDataObjectDeserializer();
        try {
            if (evento.getApiVersion() != null) {
                var seguro = deserializador.getObject();
                if (seguro.isPresent()) { return seguro.get(); }
            }
            return deserializador.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException excepcion) {
            throw new PasarelaException("No se pudo leer el objeto del evento de Stripe");
        }
    }
}

package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {
    private final EventoStripeRepository eventos;
    private final SalonRepository salones;
    private final String secretoWebhook;
    private final Clock reloj;
    private final int creditosPro;
    private final int creditosPaquete;

    public WebhookService(EventoStripeRepository eventos, SalonRepository salones,
                          @Value("${sillon.stripe.secreto-webhook}") String secretoWebhook,
                          @Value("${sillon.creditos.pro:300}") int creditosPro,
                          @Value("${sillon.creditos.paquete:200}") int creditosPaquete,
                          Clock reloj) {
        this.eventos = eventos;
        this.salones = salones;
        this.secretoWebhook = secretoWebhook;
        this.creditosPro = creditosPro;
        this.creditosPaquete = creditosPaquete;
        this.reloj = reloj;
    }

    @Transactional
    public WebhookRespuesta procesar(String cuerpo, String firma) {
        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException("Falta la firma de Stripe");
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(cuerpo, firma, secretoWebhook);
        } catch (SignatureVerificationException | IllegalArgumentException ex) {
            throw new FirmaInvalidaException("La firma de Stripe no es válida");
        }
        if (evento.getId() == null || evento.getId().isBlank()) {
            throw new PasarelaException("El evento de Stripe no tiene identificador");
        }
        if (eventos.existsById(evento.getId())) {
            return new WebhookRespuesta(true, false);
        }

        boolean procesado = switch (evento.getType()) {
            case "checkout.session.completed" -> procesarCheckout(evento);
            case "customer.subscription.deleted" -> procesarCancelacion(evento);
            default -> false;
        };
        eventos.save(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(reloj)));
        return new WebhookRespuesta(true, procesado);
    }

    private boolean procesarCheckout(Event evento) {
        StripeObject objeto = leerObjeto(evento);
        if (!(objeto instanceof Session sesion)) {
            throw new PasarelaException("Stripe no devolvió una sesión de pago válida");
        }
        if (!"paid".equals(sesion.getPaymentStatus())) {
            return false;
        }
        Map<String, String> metadata = sesion.getMetadata();
        if (metadata == null || !metadata.containsKey("salonId") || !metadata.containsKey("tipo")) {
            throw new PasarelaException("La sesión de Stripe no tiene los datos del salón y la compra");
        }
        Long salonId;
        try {
            salonId = Long.valueOf(metadata.get("salonId"));
        } catch (NumberFormatException ex) {
            throw new PasarelaException("La sesión de Stripe tiene un id de salón inválido");
        }
        Salon salon = salones.findById(salonId)
                .orElseThrow(() -> new PasarelaException("El salón del pago no existe"));
        switch (metadata.get("tipo")) {
            case "SUSCRIPCION_PRO" -> {
                if (sesion.getSubscription() == null || sesion.getSubscription().isBlank()) {
                    throw new PasarelaException("Stripe no devolvió el id de la suscripción");
                }
                salon.setPlan(Plan.PRO);
                salon.setStripeSuscripcionId(sesion.getSubscription());
                salon.setCreditosIa(salon.getCreditosIa() + creditosPro);
            }
            case "PAQUETE_CREDITOS" -> salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
            default -> throw new PasarelaException("El tipo de compra de Stripe no es válido");
        }
        salones.save(salon);
        return true;
    }

    private boolean procesarCancelacion(Event evento) {
        StripeObject objeto = leerObjeto(evento);
        if (!(objeto instanceof Subscription suscripcion)) {
            throw new PasarelaException("Stripe no devolvió una suscripción válida");
        }
        Optional<Salon> salonEncontrado = salones.findByStripeSuscripcionId(suscripcion.getId());
        if (salonEncontrado.isEmpty()) {
            return false;
        }
        Salon salon = salonEncontrado.get();
        salon.setPlan(Plan.GRATIS);
        salon.setStripeSuscripcionId(null);
        salones.save(salon);
        return true;
    }

    private StripeObject leerObjeto(Event evento) {
        try {
            return evento.getDataObjectDeserializer().getObject()
                    .orElseGet(() -> {
                        try {
                            return evento.getDataObjectDeserializer().deserializeUnsafe();
                        } catch (EventDataObjectDeserializationException ex) {
                            throw new PasarelaException("Stripe devolvió un evento que no se pudo leer");
                        }
                    });
        } catch (IllegalStateException ex) {
            throw new PasarelaException("Stripe devolvió un evento que no se pudo leer");
        }
    }
}

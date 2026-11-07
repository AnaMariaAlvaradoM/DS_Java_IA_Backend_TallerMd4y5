package com.devsenior.sillon.pago.service;
import com.devsenior.sillon.compartido.FirmaStripeInvalidaException;
import com.devsenior.sillon.compartido.ServicioExternoException;
import com.devsenior.sillon.pago.config.StripeProperties;


import com.devsenior.sillon.pago.model.EventoStripe;
import com.devsenior.sillon.pago.repository.EventoStripeRepository;

import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class PagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PagoWebhookService.class);

    private final StripeProperties stripeProperties;
    private final EventoStripeRepository eventoStripeRepository;
    private final SalonRepository salonRepository;

    public PagoWebhookService(StripeProperties stripeProperties, EventoStripeRepository eventoStripeRepository, SalonRepository salonRepository) {
        this.stripeProperties = stripeProperties;
        this.eventoStripeRepository = eventoStripeRepository;
        this.salonRepository = salonRepository;
    }

    @Transactional
    public Map<String, Boolean> procesarEvento(String payload, String signature) {
        Event event;

        // 1. Validar la firma
        try {
            event = Webhook.constructEvent(payload, signature, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Firma de Stripe inválida", e);
            throw new FirmaStripeInvalidaException("Firma de Stripe inválida");
        }

        // 2. Idempotencia: Verificar si el evento ya fue procesado
        if (eventoStripeRepository.existsById(event.getId())) {
            log.info("El evento {} ya fue procesado anteriormente", event.getId());
            return Map.of("recibido", true, "procesado", false); // Responde que no se procesó
        }

        // 3. Extraer el objeto con deserialización segura (OJO)
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;

        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialización sin verificación de versión si la API y la librería no coinciden
            log.warn("Versión de API de Stripe no coincide. Usando deserializeUnsafe para el evento {}", event.getId());
            try {
                stripeObject = dataObjectDeserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                log.error("No se pudo deserializar el evento de Stripe ni siquiera de forma forzada", e);
                // Si tienes la excepción ServicioExternoException, úsala aquí. O responde false.
                throw new ServicioExternoException("Error crítico leyendo datos de Stripe: " + e.getMessage());
            }
        }

        // 4. Lógica según el tipo de evento
        switch (event.getType()) {
            case "checkout.session.completed":
                Session session = (Session) stripeObject;
                if ("paid".equals(session.getPaymentStatus())) {
                    procesarCompra(session);
                }
                break;

            case "customer.subscription.deleted":
                Subscription subscription = (Subscription) stripeObject;
                cancelarSuscripcion(subscription.getId());
                break;

            default:
                log.info("Evento no manejado: {}", event.getType());
                break;
        }

        // 5. Guardar el evento para no volverlo a procesar
        eventoStripeRepository.save(new EventoStripe(event.getId(), event.getType()));

        return Map.of("recibido", true, "procesado", true);
    }

    private void procesarCompra(Session session) {
        Map<String, String> metadata = session.getMetadata();
        if (metadata == null || !metadata.containsKey("salonId")) return;

        Long salonId = Long.parseLong(metadata.get("salonId"));
        String tipo = metadata.get("tipo");

        Salon salon = salonRepository.findById(salonId).orElseThrow();

        if (salon.getCreditosIa() == null) {
            salon.setCreditosIa(0);
        }

        if ("SUSCRIPCION_PRO".equals(tipo)) {
            salon.setPro(true);
            // Guardamos el id de la suscripción de Stripe en el salón para poder buscarlo si se cancela
            salon.setSuscripcionStripeId(session.getSubscription());
            salon.setCreditosIa(salon.getCreditosIa() + 300);
        } else if ("PAQUETE_CREDITOS".equals(tipo)) {
            // Sumar los créditos al saldo actual del salón
            salon.setCreditosIa(salon.getCreditosIa() + 200); // Ajusta la cantidad según tu lógica
        }

        salonRepository.save(salon);
    }

    private void cancelarSuscripcion(String suscripcionStripeId) {
        // Busca el salón por el ID de la suscripción y lo devuelve a GRATIS
        salonRepository.findBySuscripcionStripeId(suscripcionStripeId)
                .ifPresent(salon -> {
                    salon.setPro(false);
                    salon.setSuscripcionStripeId(null);
                    salonRepository.save(salon);
                    log.info("El salón {} ha vuelto al plan GRATIS", salon.getId());
                });
    }
}

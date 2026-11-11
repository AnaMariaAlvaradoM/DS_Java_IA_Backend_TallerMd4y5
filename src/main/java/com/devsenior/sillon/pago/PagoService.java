package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.ConflictoExcepcion;
import com.devsenior.sillon.compartido.ErrorPasarelaException;
import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.pago.dto.PagoRespuestaDto;
import com.devsenior.sillon.pago.dto.WebhookRespuestaDto;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final StripeProperties stripeProperties;
    private final StripeClient stripeClient;
    private final Clock clock;

    @Value("${sillon.url-publica}")
    private String urlPublica;

    @Value("${sillon.creditos-pro:300}")
    private int creditosPro;

    @Value("${sillon.creditos-paquete:200}")
    private int creditosPaquete;

    public PagoService(
            SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository,
            StripeProperties stripeProperties,
            StripeClient stripeClient,
            Clock clock) {
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.stripeProperties = stripeProperties;
        this.stripeClient = stripeClient;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PagoRespuestaDto crearSesion(Long salonId, String emailDueno, TipoCompra tipo) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("Salón no encontrado"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ConflictoExcepcion("El salón ya cuenta con el plan PRO activo");
        }

        String priceId;
        SessionCreateParams.Mode modo;

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            priceId = stripeProperties.getPrecioPro();
            modo = SessionCreateParams.Mode.SUBSCRIPTION;
        } else {
            priceId = stripeProperties.getPrecioCreditos();
            modo = SessionCreateParams.Mode.PAYMENT;
        }

        String successUrl = urlPublica + "/api/pagos/resultado?estado=exito";
        String cancelUrl = urlPublica + "/api/pagos/resultado?estado=cancelado";
        String salonIdStr = salon.getId().toString();

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(modo)
                .setCustomerEmail(emailDueno)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(salonIdStr)
                .putMetadata("salonId", salonIdStr)
                .putMetadata("tipo", tipo.name())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                );

        if (modo == SessionCreateParams.Mode.SUBSCRIPTION) {
            builder.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("salonId", salonIdStr)
                            .putMetadata("tipo", tipo.name())
                            .build()
            );
        }

        try {
            Session session = stripeClient.v1().checkout().sessions().create(builder.build());
            return new PagoRespuestaDto(tipo, session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Error al interactuar con Stripe: {}", e.getMessage(), e);
            throw new ErrorPasarelaException("No fue posible procesar el pago con la pasarela");
        }
    }

    @Transactional
    public WebhookRespuestaDto procesarWebhook(String payload, String firma) {
        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException("Firma de Stripe ausente");
        }

        Event event;
        try {
            event = Webhook.constructEvent(
                    payload,
                    firma,
                    stripeProperties.getSecretoWebhook()
            );
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("Firma de Stripe inválida");
        }

        if (eventoStripeRepository.existsById(event.getId())) {
            return new WebhookRespuestaDto(true, false);
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = deserializer.getObject().orElseGet(() -> {
            try {
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException e) {
                log.warn("Fallo al deserializar StripeObject mediante deserializeUnsafe: {}", e.getMessage());
                return null;
            }
        });

        boolean procesado = false;

        if ("checkout.session.completed".equals(event.getType())) {
            if (stripeObject instanceof Session session) {
                if ("paid".equalsIgnoreCase(session.getPaymentStatus())) {
                    procesado = procesarCheckoutCompletado(session);
                } else {
                    log.warn("Evento checkout.session.completed recibido con estado de pago no completado: {}", session.getPaymentStatus());
                }
            }
        } else if ("customer.subscription.deleted".equals(event.getType())) {
            if (stripeObject instanceof Subscription subscription) {
                procesado = procesarSuscripcionCancelada(subscription);
            }
        } else {
            log.info("Evento Stripe ignorable de tipo: {}", event.getType());
        }

        eventoStripeRepository.save(new EventoStripe(
                event.getId(),
                event.getType(),
                LocalDateTime.now(clock)
        ));

        return new WebhookRespuestaDto(true, procesado);
    }

    private boolean procesarCheckoutCompletado(Session session) {
        String salonIdStr = session.getClientReferenceId();
        if (salonIdStr == null && session.getMetadata() != null) {
            salonIdStr = session.getMetadata().get("salonId");
        }

        if (salonIdStr == null) {
            log.warn("No se encontró metadata ni clientReferenceId con salonId en la sesión {}", session.getId());
            return false;
        }

        Long salonId = Long.parseLong(salonIdStr);
        Salon salon = salonRepository.findById(salonId).orElse(null);
        if (salon == null) {
            log.warn("Salón con ID {} no encontrado al procesar checkout.session.completed", salonId);
            return false;
        }

        String tipoStr = session.getMetadata() != null ? session.getMetadata().get("tipo") : null;

        if (TipoCompra.SUSCRIPCION_PRO.name().equals(tipoStr) || "SUBSCRIPTION".equalsIgnoreCase(session.getMode())) {
            salon.setPlan(Plan.PRO);
            if (session.getSubscription() != null) {
                salon.setStripeSuscripcionId(session.getSubscription());
            }
            salon.setCreditosIa(salon.getCreditosIa() + creditosPro);
            salonRepository.save(salon);
            return true;
        } else if (TipoCompra.PAQUETE_CREDITOS.name().equals(tipoStr) || "PAYMENT".equalsIgnoreCase(session.getMode())) {
            salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
            salonRepository.save(salon);
            return true;
        }

        log.warn("Tipo de compra desconocido en metadata: {}", tipoStr);
        return false;
    }

    private boolean procesarSuscripcionCancelada(Subscription subscription) {
        Salon salon = salonRepository.findByStripeSuscripcionId(subscription.getId()).orElse(null);
        if (salon == null && subscription.getMetadata() != null && subscription.getMetadata().containsKey("salonId")) {
            Long salonId = Long.parseLong(subscription.getMetadata().get("salonId"));
            salon = salonRepository.findById(salonId).orElse(null);
        }

        if (salon != null) {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSuscripcionId(null);
            salonRepository.save(salon);
            return true;
        }

        log.warn("No se encontró el salón asociado a la suscripción cancelada: {}", subscription.getId());
        return false;
    }
}
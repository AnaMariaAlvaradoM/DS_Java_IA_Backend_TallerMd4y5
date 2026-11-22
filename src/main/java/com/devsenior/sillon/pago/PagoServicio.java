package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.NegocioConfig;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoServicio {

    private final StripeClient stripeClient;
    private final StripeConfig stripeConfig;
    private final NegocioConfig negocioConfig;
    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final Clock reloj;
    private final String urlPublica;

    public PagoServicio(
            StripeClient stripeClient,
            StripeConfig stripeConfig,
            NegocioConfig negocioConfig,
            SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository,
            Clock reloj,
            @Value("${sillon.url-publica:http://localhost:8080}") String urlPublica) {
        this.stripeClient = stripeClient;
        this.stripeConfig = stripeConfig;
        this.negocioConfig = negocioConfig;
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.reloj = reloj;
        this.urlPublica = limpiarUrl(urlPublica);
    }

    private String limpiarUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8080";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public SesionPagoRespuesta crearSesionSuscripcion(Long salonId, String emailUsuario) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya cuenta con el plan PRO activo.");
        }

        String idStr = (salon.getId() != null ? salon.getId() : salonId).toString();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(emailUsuario)
                .setClientReferenceId(idStr)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&sesion_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(stripeConfig.precioPro())
                        .setQuantity(1L)
                        .build())
                .putMetadata("salonId", idStr)
                .putMetadata("tipoCompra", TipoCompra.SUSCRIPCION_PRO.name())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("salonId", idStr)
                        .putMetadata("tipoCompra", TipoCompra.SUSCRIPCION_PRO.name())
                        .build())
                .build();

        try {
            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.SUSCRIPCION_PRO, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al crear la sesión de suscripción en Stripe: " + e.getMessage());
        }
    }

    public SesionPagoRespuesta crearSesionCreditos(Long salonId, String emailUsuario) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        String idStr = (salon.getId() != null ? salon.getId() : salonId).toString();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(emailUsuario)
                .setClientReferenceId(idStr)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&sesion_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(stripeConfig.precioCreditos())
                        .setQuantity(1L)
                        .build())
                .putMetadata("salonId", idStr)
                .putMetadata("tipoCompra", TipoCompra.PAQUETE_CREDITOS.name())
                .build();

        try {
            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.PAQUETE_CREDITOS, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al crear la sesión de compra de créditos en Stripe: " + e.getMessage());
        }
    }

    public WebhookRespuesta procesarWebhook(String payload, String sigHeader) {
        if (sigHeader == null || sigHeader.isBlank()) {
            throw new FirmaInvalidaException("Falta la cabecera Stripe-Signature");
        }
        if (payload == null || payload.isBlank()) {
            throw new FirmaInvalidaException("El cuerpo del webhook está vacío");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.secretoWebhook());
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("Firma de webhook de Stripe inválida: " + e.getMessage());
        } catch (Exception e) {
            throw new FirmaInvalidaException("Error al verificar la firma del webhook: " + e.getMessage());
        }

        return procesarEvento(event);
    }

    @Transactional
    public WebhookRespuesta procesarEvento(Event event) {
        if (eventoStripeRepository.existsById(event.getId())) {
            return new WebhookRespuesta(true, false, "El evento ya fue procesado previamente");
        }

        String tipoEvento = event.getType();
        if ("checkout.session.completed".equals(tipoEvento)) {
            procesarCheckoutSessionCompleted(event);
        } else if ("customer.subscription.deleted".equals(tipoEvento)) {
            procesarCustomerSubscriptionDeleted(event);
        }

        EventoStripe eventoStripe = new EventoStripe(event.getId(), event.getType(), LocalDateTime.now(reloj));
        eventoStripeRepository.save(eventoStripe);

        return new WebhookRespuesta(true, true, "Evento procesado con éxito");
    }

    private void procesarCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElseGet(() -> {
            try {
                return dataObjectDeserializer.deserializeUnsafe();
            } catch (Exception e) {
                return null;
            }
        });

        if (!(stripeObject instanceof Session session)) {
            return;
        }

        // Si el estado de pago no es "paid", se descarta
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        String salonIdStr = metadata != null ? metadata.get("salonId") : null;
        if (salonIdStr == null && session.getClientReferenceId() != null) {
            salonIdStr = session.getClientReferenceId();
        }

        String tipoCompraStr = metadata != null ? metadata.get("tipoCompra") : null;
        if (tipoCompraStr == null && session.getMode() != null) {
            if ("subscription".equalsIgnoreCase(session.getMode())) {
                tipoCompraStr = TipoCompra.SUSCRIPCION_PRO.name();
            } else if ("payment".equalsIgnoreCase(session.getMode())) {
                tipoCompraStr = TipoCompra.PAQUETE_CREDITOS.name();
            }
        }

        if (salonIdStr == null || tipoCompraStr == null) {
            return;
        }

        Long salonId;
        try {
            salonId = Long.parseLong(salonIdStr);
        } catch (NumberFormatException e) {
            return;
        }

        Salon salon = salonRepository.findById(salonId).orElse(null);
        if (salon == null) {
            return;
        }

        TipoCompra tipoCompra;
        try {
            tipoCompra = TipoCompra.valueOf(tipoCompraStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        if (tipoCompra == TipoCompra.SUSCRIPCION_PRO) {
            salon.setPlan(Plan.PRO);
            salon.setCreditosIa(salon.getCreditosIa() + negocioConfig.creditosPlanPro());
            String subId = session.getSubscription();
            if (subId == null && session.getSubscriptionObject() != null) {
                subId = session.getSubscriptionObject().getId();
            }
            if (subId != null && !subId.isBlank()) {
                salon.setStripeSuscripcionId(subId);
            }
            salonRepository.save(salon);
        } else if (tipoCompra == TipoCompra.PAQUETE_CREDITOS) {
            salon.setCreditosIa(salon.getCreditosIa() + negocioConfig.creditosPaquete());
            salonRepository.save(salon);
        }
    }

    private void procesarCustomerSubscriptionDeleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElseGet(() -> {
            try {
                return dataObjectDeserializer.deserializeUnsafe();
            } catch (Exception e) {
                return null;
            }
        });

        if (!(stripeObject instanceof Subscription subscription)) {
            return;
        }

        String subId = subscription.getId();
        Optional<Salon> salonOpt = Optional.empty();
        if (subId != null && !subId.isBlank()) {
            salonOpt = salonRepository.findByStripeSuscripcionId(subId);
        }

        if (salonOpt.isEmpty() && subscription.getMetadata() != null && subscription.getMetadata().containsKey("salonId")) {
            try {
                Long salonId = Long.parseLong(subscription.getMetadata().get("salonId"));
                salonOpt = salonRepository.findById(salonId);
            } catch (NumberFormatException ignored) {
            }
        }

        salonOpt.ifPresent(salon -> {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSuscripcionId(null);
            salonRepository.save(salon);
        });
    }

    public ResultadoPagoRespuesta obtenerResultado(String estado) {
        if ("exito".equalsIgnoreCase(estado)) {
            return new ResultadoPagoRespuesta("El pago fue completado con éxito. Ya puedes disfrutar de los beneficios.");
        } else if ("cancelado".equalsIgnoreCase(estado)) {
            return new ResultadoPagoRespuesta("El proceso de pago fue cancelado.");
        }
        return new ResultadoPagoRespuesta("Resultado de pago procesado.");
    }
}

package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.ErrorPasarelaException;
import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
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

@Service
public class PagoService {

    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final StripeClient stripeClient;
    private final StripeProperties stripeProperties;
    private final Clock clock;
    private final String urlPublica;
    private final int creditosPlanPro;
    private final int creditosPaquete;

    public PagoService(SalonRepository salonRepository, EventoStripeRepository eventoStripeRepository,
            StripeClient stripeClient, StripeProperties stripeProperties, Clock clock,
            @Value("${sillon.url-publica}") String urlPublica,
            @Value("${sillon.creditos.plan-pro}") int creditosPlanPro,
            @Value("${sillon.creditos.paquete}") int creditosPaquete) {
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.stripeClient = stripeClient;
        this.stripeProperties = stripeProperties;
        this.clock = clock;
        this.urlPublica = urlPublica;
        this.creditosPlanPro = creditosPlanPro;
        this.creditosPaquete = creditosPaquete;
    }

    public SesionPagoRespuesta crearSesion(UsuarioPrincipal principal, TipoCompra tipo) {
        Salon salon = salonRepository.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salon ya tiene el plan PRO");
        }

        String salonId = String.valueOf(salon.getId());
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(tipo == TipoCompra.SUSCRIPCION_PRO ? SessionCreateParams.Mode.SUBSCRIPTION
                        : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(principal.getUsername())
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setClientReferenceId(salonId)
                .putMetadata("salonId", salonId)
                .putMetadata("tipo", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(tipo == TipoCompra.SUSCRIPCION_PRO ? stripeProperties.getPrecioPro()
                                : stripeProperties.getPrecioCreditos())
                        .setQuantity(1L)
                        .build());

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            builder.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", salonId)
                    .putMetadata("tipo", tipo.name())
                    .build());
        }

        try {
            Session session = stripeClient.checkout().sessions().create(builder.build());
            return new SesionPagoRespuesta(tipo, session.getId(), session.getUrl());
        } catch (StripeException ex) {
            throw new ErrorPasarelaException("No se pudo crear la sesion de pago con Stripe");
        }
    }

    @Transactional
    public WebhookRespuesta procesarWebhook(String cuerpo, String firma) {
        if (firma == null) {
            throw new FirmaInvalidaException("Falta la firma de Stripe");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(cuerpo, firma, stripeProperties.getSecretoWebhook());
        } catch (SignatureVerificationException ex) {
            throw new FirmaInvalidaException("La firma de Stripe no es valida");
        }

        if (eventoStripeRepository.existsById(evento.getId())) {
            return new WebhookRespuesta(true, false);
        }

        boolean procesado = switch (evento.getType()) {
            case "checkout.session.completed" -> {
                procesarSesionCompletada(deserializar(evento, Session.class));
                yield true;
            }
            case "customer.subscription.deleted" -> {
                procesarSuscripcionCancelada(deserializar(evento, Subscription.class));
                yield true;
            }
            default -> false;
        };

        if (!procesado) {
            return new WebhookRespuesta(true, false);
        }

        eventoStripeRepository.save(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(clock)));
        return new WebhookRespuesta(true, true);
    }

    private void procesarSesionCompletada(Session session) {
        if (!"paid".equals(session.getPaymentStatus())) {
            return;
        }

        Map<String, String> metadata = session.getMetadata();
        Long salonId = Long.valueOf(metadata.get("salonId"));
        TipoCompra tipo = TipoCompra.valueOf(metadata.get("tipo"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            Salon salon = salonRepository.findById(salonId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(session.getSubscription());
            salonRepository.save(salon);
            salonRepository.sumarCreditos(salonId, creditosPlanPro);
        } else {
            salonRepository.sumarCreditos(salonId, creditosPaquete);
        }
    }

    private void procesarSuscripcionCancelada(Subscription subscription) {
        Salon salon = salonRepository.findByStripeSuscripcionId(subscription.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salon no encontrado"));
        salon.setPlan(Plan.GRATIS);
        salonRepository.save(salon);
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T deserializar(Event evento, Class<T> tipo) {
        EventDataObjectDeserializer deserializador = evento.getDataObjectDeserializer();
        Optional<StripeObject> objeto = deserializador.getObject();
        if (objeto.isPresent()) {
            return (T) objeto.get();
        }
        try {
            // La version de API del evento puede no coincidir con la de la libreria;
            // se deserializa sin verificarla.
            return (T) deserializador.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException ex) {
            throw new ErrorPasarelaException("No se pudo leer el evento de Stripe");
        }
    }
}

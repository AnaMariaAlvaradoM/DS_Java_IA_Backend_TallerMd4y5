package com.devsenior.sillon.pago.servicio;

import com.devsenior.sillon.compartido.excepcion.ErrorPasarelaException;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.SesionPagoResponse;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.modelo.EventoStripe;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepository;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class PagoService {

    private final StripeClient stripeClient;
    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final Clock clock;
    private final String secretoWebhook;
    private final String precioPro;
    private final String precioCreditos;
    private final String urlPublica;
    private final int creditosPlanPro;
    private final int creditosPaquete;

    public PagoService(
            StripeClient stripeClient,
            SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository,
            Clock clock,
            @Value("${sillon.stripe.secreto-webhook}") String secretoWebhook,
            @Value("${sillon.stripe.precio-pro}") String precioPro,
            @Value("${sillon.stripe.precio-creditos}") String precioCreditos,
            @Value("${sillon.url-publica}") String urlPublica,
            @Value("${sillon.negocio.creditos-plan-pro}") int creditosPlanPro,
            @Value("${sillon.negocio.creditos-paquete}") int creditosPaquete) {
        this.stripeClient = stripeClient;
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.clock = clock;
        this.secretoWebhook = secretoWebhook;
        this.precioPro = precioPro;
        this.precioCreditos = precioCreditos;
        this.urlPublica = urlPublica;
        this.creditosPlanPro = creditosPlanPro;
        this.creditosPaquete = creditosPaquete;
    }

    public SesionPagoResponse crearSesionPago(UsuarioPrincipal usuario, TipoCompra tipo) {
        Salon salon = salonRepository.findById(usuario.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya tiene activo el plan PRO");
        }

        String precioId = tipo == TipoCompra.SUSCRIPCION_PRO ? precioPro : precioCreditos;
        SessionCreateParams.Mode modo = tipo == TipoCompra.SUSCRIPCION_PRO
                ? SessionCreateParams.Mode.SUBSCRIPTION
                : SessionCreateParams.Mode.PAYMENT;

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(modo)
                .setCustomerEmail(usuario.getUsername())
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setClientReferenceId(salon.getId().toString())
                .putMetadata("salonId", salon.getId().toString())
                .putMetadata("tipoCompra", tipo.name())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(precioId)
                                .setQuantity(1L)
                                .build());

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            params.setSubscriptionData(
                    SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("salonId", salon.getId().toString())
                            .putMetadata("tipoCompra", tipo.name())
                            .build());
        }

        try {
            Session session = stripeClient.checkout().sessions().create(params.build());
            return new SesionPagoResponse(tipo, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new ErrorPasarelaException("Error al crear sesión de pago en Stripe: " + e.getMessage());
        }
    }

    @Transactional
    public WebhookResponse procesarWebhook(String payload, String firmaStripe) {
        if (firmaStripe == null || firmaStripe.isBlank()) {
            throw new FirmaInvalidaException("Falta la cabecera Stripe-Signature");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(payload, firmaStripe, secretoWebhook);
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("Firma de Stripe inválida");
        } catch (Exception e) {
            throw new FirmaInvalidaException("Evento de Stripe inválido");
        }

        if (eventoStripeRepository.existsById(evento.getId())) {
            return new WebhookResponse(true, false);
        }

        StripeObject objeto = extraerObjeto(evento);

        if ("checkout.session.completed".equals(evento.getType()) && objeto instanceof Session session) {
            aplicarCheckoutCompletado(session);
        } else if ("customer.subscription.deleted".equals(evento.getType()) && objeto instanceof Subscription sub) {
            aplicarCancelacionSuscripcion(sub);
        }

        eventoStripeRepository.save(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(clock)));
        return new WebhookResponse(true, true);
    }

    private void aplicarCheckoutCompletado(Session session) {
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }
        String salonIdStr = session.getMetadata() != null ? session.getMetadata().get("salonId") : null;
        if (salonIdStr == null) {
            salonIdStr = session.getClientReferenceId();
        }
        String tipoCompraStr = session.getMetadata() != null ? session.getMetadata().get("tipoCompra") : null;
        if (salonIdStr == null || tipoCompraStr == null) {
            return;
        }

        Long salonId = Long.parseLong(salonIdStr);
        TipoCompra tipo = TipoCompra.valueOf(tipoCompraStr);

        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            salon.setPlan(Plan.PRO);
            salon.setCreditosIa(salon.getCreditosIa() + creditosPlanPro);
            if (session.getSubscription() != null) {
                salon.setStripeSubscriptionId(session.getSubscription());
            }
        } else if (tipo == TipoCompra.PAQUETE_CREDITOS) {
            salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
        }

        salonRepository.save(salon);
    }

    private void aplicarCancelacionSuscripcion(Subscription subscription) {
        salonRepository.findByStripeSubscriptionId(subscription.getId()).ifPresent(salon -> {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSubscriptionId(null);
            salonRepository.save(salon);
        });
    }

    private StripeObject extraerObjeto(Event evento) {
        EventDataObjectDeserializer deserializer = evento.getDataObjectDeserializer();
        try {
            if (deserializer.getObject().isPresent()) {
                return deserializer.getObject().get();
            }
        } catch (Exception ignored) {
            // El SDK de Stripe lanza NPE en getObject() si api_version es nulo (común en eventos de prueba)
        }
        try {
            return deserializer.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            return null;
        }
    }
}
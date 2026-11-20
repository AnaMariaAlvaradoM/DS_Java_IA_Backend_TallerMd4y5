package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.ResultadoPagoRespuesta;
import com.devsenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devsenior.sillon.pago.dto.WebhookRespuesta;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
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

    private final String precioPro;
    private final String precioCreditos;
    private final String secretoWebhook;
    private final String urlPublica;

    public PagoService(
            StripeClient stripeClient,
            SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository,
            Clock clock,
            @Value("${sillon.stripe.precio-pro}") String precioPro,
            @Value("${sillon.stripe.precio-creditos}") String precioCreditos,
            @Value("${sillon.stripe.secreto-webhook}") String secretoWebhook,
            @Value("${sillon.url-publica:http://localhost:8080}") String urlPublica
    ) {
        this.stripeClient = stripeClient;
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.clock = clock;
        this.precioPro = precioPro;
        this.precioCreditos = precioCreditos;
        this.secretoWebhook = secretoWebhook;
        this.urlPublica = urlPublica;
    }

    @Transactional(readOnly = true)
    public SesionPagoRespuesta crearSesionSuscripcion(UsuarioPrincipal principal) {
        Salon salon = salonRepository.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        if (salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya cuenta con el plan PRO activo");
        }

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomerEmail(principal.getUsername())
                    .setClientReferenceId(String.valueOf(salon.getId()))
                    .putMetadata("salonId", String.valueOf(salon.getId()))
                    .putMetadata("tipoCompra", TipoCompra.SUSCRIPCION_PRO.name())
                    .setSubscriptionData(
                            SessionCreateParams.SubscriptionData.builder()
                                    .putMetadata("salonId", String.valueOf(salon.getId()))
                                    .putMetadata("tipoCompra", TipoCompra.SUSCRIPCION_PRO.name())
                                    .build()
                    )
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(precioPro)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                    .build();

            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.SUSCRIPCION_PRO, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al comunicarse con Stripe: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public SesionPagoRespuesta crearSesionCreditos(UsuarioPrincipal principal) {
        Salon salon = salonRepository.findById(principal.getSalonId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(principal.getUsername())
                    .setClientReferenceId(String.valueOf(salon.getId()))
                    .putMetadata("salonId", String.valueOf(salon.getId()))
                    .putMetadata("tipoCompra", TipoCompra.PAQUETE_CREDITOS.name())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(precioCreditos)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                    .build();

            Session session = stripeClient.checkout().sessions().create(params);
            return new SesionPagoRespuesta(TipoCompra.PAQUETE_CREDITOS, session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PasarelaException("Error al comunicarse con Stripe: " + e.getMessage());
        }
    }

    @Transactional
    public WebhookRespuesta procesarWebhook(String payload, String sigHeader) {
        if (sigHeader == null || sigHeader.isBlank()) {
            throw new FirmaInvalidaException("Firma de webhook de Stripe ausente o inválida");
        }

        Event evento;
        try {
            evento = Webhook.constructEvent(payload, sigHeader, secretoWebhook);
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("Firma de webhook de Stripe no verificada: " + e.getMessage());
        } catch (Exception e) {
            throw new FirmaInvalidaException("Error al procesar firma de webhook: " + e.getMessage());
        }

        // Idempotencia: no procesar dos veces el mismo evento
        if (eventoStripeRepository.existsById(evento.getId())) {
            return new WebhookRespuesta(true, false);
        }

        if ("checkout.session.completed".equals(evento.getType())) {
            Session session = null;
            try {
                session = (Session) evento.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception e) {
                if (evento.getDataObjectDeserializer().getObject().isPresent()) {
                    session = (Session) evento.getDataObjectDeserializer().getObject().get();
                }
            }
            if (session != null) {
                String paymentStatus = session.getPaymentStatus();
                if ("paid".equalsIgnoreCase(paymentStatus) || paymentStatus == null) {
                    String salonIdStr = session.getMetadata() != null ? session.getMetadata().get("salonId") : null;
                    String tipoCompraStr = session.getMetadata() != null ? session.getMetadata().get("tipoCompra") : null;

                    if (salonIdStr != null) {
                        Salon salon = salonRepository.findById(Long.parseLong(salonIdStr)).orElse(null);
                        if (salon != null) {
                            if (TipoCompra.SUSCRIPCION_PRO.name().equals(tipoCompraStr)) {
                                salon.setPlan(Plan.PRO);
                                if (session.getSubscription() != null) {
                                    salon.setStripeSuscripcionId(session.getSubscription());
                                }
                                salon.setCreditosIa(salon.getCreditosIa() + 300);
                                salonRepository.save(salon);
                            } else if (TipoCompra.PAQUETE_CREDITOS.name().equals(tipoCompraStr)) {
                                salon.setCreditosIa(salon.getCreditosIa() + 200);
                                salonRepository.save(salon);
                            }
                        }
                    }
                }
            }
        } else if ("customer.subscription.deleted".equals(evento.getType())) {
            Subscription subscription = null;
            try {
                subscription = (Subscription) evento.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception e) {
                if (evento.getDataObjectDeserializer().getObject().isPresent()) {
                    subscription = (Subscription) evento.getDataObjectDeserializer().getObject().get();
                }
            }
            if (subscription != null && subscription.getId() != null) {
                salonRepository.findByStripeSuscripcionId(subscription.getId()).ifPresent(salon -> {
                    salon.setPlan(Plan.GRATIS);
                    salon.setStripeSuscripcionId(null);
                    salonRepository.save(salon);
                });
            }
        }

        eventoStripeRepository.save(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(clock)));
        return new WebhookRespuesta(true, true);
    }

    public ResultadoPagoRespuesta obtenerResultado(String estado) {
        String mensaje = "exito".equalsIgnoreCase(estado)
                ? "¡Pago procesado exitosamente! Tu cuenta ha sido actualizada."
                : "El proceso de pago fue cancelado. No se realizó ningún cobro.";
        return new ResultadoPagoRespuesta(estado, mensaje);
    }
}

package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final long TOLERANCIA_SEGUNDOS = 300;

    private final StripeClient stripeClient;
    private final SalonRepository salonRepository;
    private final EventoStripeRepository eventoStripeRepository;
    private final Clock clock;
    private final String precioPro;
    private final String precioCreditos;
    private final String secretoWebhook;
    private final String urlPublica;
    private final int creditosPro;
    private final int creditosPaquete;

    public PagoService(StripeClient stripeClient, SalonRepository salonRepository,
            EventoStripeRepository eventoStripeRepository, Clock clock,
            @Value("${sillon.stripe.precio-pro}") String precioPro,
            @Value("${sillon.stripe.precio-creditos}") String precioCreditos,
            @Value("${sillon.stripe.secreto-webhook}") String secretoWebhook,
            @Value("${sillon.url-publica}") String urlPublica,
            @Value("${sillon.creditos.pro:300}") int creditosPro,
            @Value("${sillon.creditos.paquete:200}") int creditosPaquete) {
        this.stripeClient = stripeClient;
        this.salonRepository = salonRepository;
        this.eventoStripeRepository = eventoStripeRepository;
        this.clock = clock;
        this.precioPro = precioPro;
        this.precioCreditos = precioCreditos;
        this.secretoWebhook = secretoWebhook;
        this.urlPublica = urlPublica.replaceAll("/+$", "");
        this.creditosPro = creditosPro;
        this.creditosPaquete = creditosPaquete;
    }

    /**
     * El backend decide el precio: el cliente de la API solo dice qué quiere comprar.
     * Si el salón ya es PRO, la suscripción responde 409 antes de llamar a Stripe.
     */
    public PagoRespuesta crearSesion(Long salonId, String emailDueno, TipoCompra tipo) {
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new NoEncontradoException("El salón no existe"));
        if (tipo == TipoCompra.SUSCRIPCION_PRO && salon.getPlan() == Plan.PRO) {
            throw new ReglaNegocioException("El salón ya tiene el plan PRO");
        }

        boolean esSuscripcion = tipo == TipoCompra.SUSCRIPCION_PRO;
        String idSalon = String.valueOf(salonId);
        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(esSuscripcion ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(emailDueno)
                .setSuccessUrl(urlPublica + "/api/pagos/resultado?estado=exito")
                .setCancelUrl(urlPublica + "/api/pagos/resultado?estado=cancelado")
                .setClientReferenceId(idSalon)
                .putMetadata("salonId", idSalon)
                .putMetadata("tipo", tipo.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(esSuscripcion ? precioPro : precioCreditos)
                        .setQuantity(1L)
                        .build());
        if (esSuscripcion) {
            params.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .putMetadata("salonId", idSalon)
                    .putMetadata("tipo", tipo.name())
                    .build());
        }

        try {
            Session sesion = stripeClient.v1().checkout().sessions().create(params.build());
            if (sesion.getUrl() == null) {
                throw new PasarelaException("Stripe no devolvió la URL de pago");
            }
            return new PagoRespuesta(tipo, sesion.getId(), sesion.getUrl());
        } catch (StripeException e) {
            log.error("Stripe rechazó la creación de la sesión de pago: {}", e.getMessage());
            throw new PasarelaException("No se pudo crear la sesión de pago en Stripe");
        }
    }

    /**
     * Webhook: 1) verifica la firma (400 si falta o es falsa), 2) ignora eventos repetidos,
     * 3) aplica el evento, 4) guarda el evento. Todo en una transacción: si algo falla,
     * no queda ni el cambio ni el evento, y Stripe lo reintenta.
     */
    @Transactional
    public WebhookRespuesta procesarWebhook(String payload, String firma) {
        if (firma == null || firma.isBlank() || payload == null) {
            throw new FirmaInvalidaException("Falta la firma de Stripe (cabecera Stripe-Signature)");
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(payload, firma, secretoWebhook, TOLERANCIA_SEGUNDOS);
        } catch (SignatureVerificationException e) {
            throw new FirmaInvalidaException("La firma del webhook no es válida");
        } catch (RuntimeException e) {
            throw new FirmaInvalidaException("El cuerpo del webhook no es un evento válido de Stripe");
        }
        String eventoId = evento.getId();
        String tipoEvento = evento.getType();
        if (eventoId == null || tipoEvento == null) {
            throw new FirmaInvalidaException("El evento no tiene id o tipo");
        }

        if (eventoStripeRepository.existsById(eventoId)) {
            log.info("Evento {} repetido: se ignora", eventoId);
            return new WebhookRespuesta(true, false);
        }

        switch (tipoEvento) {
            case "checkout.session.completed" -> {
                if (leerObjeto(evento) instanceof Session sesion) {
                    aplicarCompra(sesion);
                }
            }
            case "customer.subscription.deleted" -> {
                if (leerObjeto(evento) instanceof Subscription suscripcion) {
                    cancelarSuscripcion(suscripcion.getId());
                }
            }
            default -> log.info("Evento {} de tipo {}: no requiere acción", eventoId, tipoEvento);
        }

        eventoStripeRepository.save(new EventoStripe(eventoId, tipoEvento, LocalDateTime.now(clock)));
        return new WebhookRespuesta(true, true);
    }

    /**
     * Si la versión de la API de la cuenta no coincide con la de la librería, el objeto puede venir
     * vacío al deserializar de forma segura: por eso se usa deserializeUnsafe (ver bitácora).
     */
    private StripeObject leerObjeto(Event evento) {
        try {
            return evento.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            log.error("No se pudo leer el objeto del evento {}: {}", evento.getId(), e.getMessage());
            throw new PasarelaException("No se pudo leer el evento de Stripe");
        }
    }

    private void aplicarCompra(Session sesion) {
        if (!"paid".equals(sesion.getPaymentStatus())) {
            log.info("Sesión {} sin pago confirmado: no se aplica", sesion.getId());
            return;
        }
        Map<String, String> metadata = sesion.getMetadata() == null ? Map.of() : sesion.getMetadata();
        String salonId = metadata.get("salonId");
        String tipo = metadata.get("tipo");
        if (salonId == null || tipo == null) {
            log.warn("Sesión {} sin metadata de salón o tipo", sesion.getId());
            return;
        }
        Salon salon = salonRepository.findById(Long.valueOf(salonId)).orElse(null);
        if (salon == null) {
            log.warn("El salón {} del pago no existe", salonId);
            return;
        }
        if (TipoCompra.SUSCRIPCION_PRO.name().equals(tipo)) {
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(sesion.getSubscription());
            salon.setCreditosIa(salon.getCreditosIa() + creditosPro);
        } else if (TipoCompra.PAQUETE_CREDITOS.name().equals(tipo)) {
            salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
        }
        salonRepository.save(salon);
    }

    /** Suscripción cancelada en Stripe: vuelve a GRATIS y conserva sus créditos. */
    private void cancelarSuscripcion(String suscripcionId) {
        if (suscripcionId == null) {
            return;
        }
        salonRepository.findByStripeSuscripcionId(suscripcionId).ifPresent(salon -> {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSuscripcionId(null);
            salonRepository.save(salon);
        });
    }
}

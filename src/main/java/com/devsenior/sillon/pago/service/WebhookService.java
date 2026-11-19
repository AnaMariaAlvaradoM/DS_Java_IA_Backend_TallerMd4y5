package com.devsenior.sillon.pago.service;

import com.devsenior.sillon.compartido.config.NegocioProperties;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.pago.config.StripeProperties;
import com.devsenior.sillon.pago.dto.EventoWebhook;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.model.EventoStripe;
import com.devsenior.sillon.pago.model.TipoCompra;
import com.devsenior.sillon.pago.repository.EventoStripeRepository;
import com.devsenior.sillon.salon.model.Plan;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Webhook de Stripe: verifica la firma sobre el cuerpo crudo, procesa cada evento una sola vez
 * y aplica la compra. Todo en una transacción: si algo falla, no queda ni el cambio ni el evento,
 * y Stripe lo reintenta.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final long TOLERANCIA_SEGUNDOS = 300;
    static final String PAGO_COMPLETADO = "checkout.session.completed";
    static final String SUSCRIPCION_CANCELADA = "customer.subscription.deleted";

    private final EventoStripeRepository eventoStripeRepository;
    private final SalonRepository salonRepository;
    private final ObjectMapper objectMapper;
    private final StripeProperties stripe;
    private final NegocioProperties negocio;
    private final Clock clock;

    public WebhookService(EventoStripeRepository eventoStripeRepository, SalonRepository salonRepository,
                          ObjectMapper objectMapper, StripeProperties stripe, NegocioProperties negocio, Clock clock) {
        this.eventoStripeRepository = eventoStripeRepository;
        this.salonRepository = salonRepository;
        this.objectMapper = objectMapper;
        this.stripe = stripe;
        this.negocio = negocio;
        this.clock = clock;
    }

    @Transactional
    public WebhookResponse procesar(String cuerpo, String firma) {
        verificarFirma(cuerpo, firma);
        EventoWebhook evento = leer(cuerpo);

        if (eventoStripeRepository.existsById(evento.id())) {
            log.info("Evento de Stripe {} repetido: se ignora", evento.id());
            return new WebhookResponse(true, false);
        }

        switch (evento.type()) {
            case PAGO_COMPLETADO -> aplicarPago(evento);
            case SUSCRIPCION_CANCELADA -> aplicarCancelacion(evento);
            default -> log.info("Evento de Stripe {} de tipo {}: no requiere acción", evento.id(), evento.type());
        }

        eventoStripeRepository.save(new EventoStripe(evento.id(), evento.type(), LocalDateTime.now(clock)));
        return new WebhookResponse(true, true);
    }

    private void verificarFirma(String cuerpo, String firma) {
        if (firma == null || firma.isBlank() || cuerpo == null) {
            throw new FirmaInvalidaException("Falta la firma de Stripe (cabecera Stripe-Signature)");
        }
        try {
            Webhook.Signature.verifyHeader(cuerpo, firma, stripe.secretoWebhook(), TOLERANCIA_SEGUNDOS);
        } catch (SignatureVerificationException ex) {
            throw new FirmaInvalidaException("La firma de Stripe no es válida");
        }
    }

    private EventoWebhook leer(String cuerpo) {
        try {
            EventoWebhook evento = objectMapper.readValue(cuerpo, EventoWebhook.class);
            if (evento.id() == null || evento.type() == null) {
                throw new FirmaInvalidaException("El evento de Stripe no trae id o tipo");
            }
            return evento;
        } catch (JacksonException ex) {
            throw new FirmaInvalidaException("El cuerpo del evento de Stripe no es un JSON válido");
        }
    }

    /** checkout.session.completed pagado: PRO activa el plan, guarda la suscripción y suma 300; paquete suma 200. */
    private void aplicarPago(EventoWebhook evento) {
        EventoWebhook.Objeto sesion = evento.objeto();
        if (sesion == null || !"paid".equals(sesion.estadoPago()) || sesion.metadata() == null) {
            log.warn("Evento {} sin pago confirmado o sin metadata: no se aplica", evento.id());
            return;
        }
        Map<String, String> metadata = sesion.metadata();
        Salon salon = buscarSalon(metadata.get("salonId"));
        if (salon == null) {
            log.warn("Evento {}: el salón de la metadata no existe", evento.id());
            return;
        }
        TipoCompra tipo = leerTipo(metadata.get("tipo"));
        if (tipo == null) {
            log.warn("Evento {}: tipo de compra desconocido en la metadata", evento.id());
            return;
        }
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(sesion.subscription());
            salon.setCreditosIa(salon.getCreditosIa() + negocio.creditosPro());
        } else {
            salon.setCreditosIa(salon.getCreditosIa() + negocio.creditosPaquete());
        }
        salonRepository.save(salon);
        log.info("Evento {}: salón {} recibió {}", evento.id(), salon.getId(), tipo);
    }

    /** customer.subscription.deleted: el salón vuelve a GRATIS y conserva sus créditos. */
    private void aplicarCancelacion(EventoWebhook evento) {
        EventoWebhook.Objeto suscripcion = evento.objeto();
        if (suscripcion == null || suscripcion.id() == null) {
            return;
        }
        salonRepository.findByStripeSuscripcionId(suscripcion.id()).ifPresent(salon -> {
            salon.setPlan(Plan.GRATIS);
            salonRepository.save(salon);
            log.info("Evento {}: salón {} vuelve a GRATIS", evento.id(), salon.getId());
        });
    }

    private TipoCompra leerTipo(String tipo) {
        try {
            return tipo == null ? null : TipoCompra.valueOf(tipo);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Salon buscarSalon(String salonId) {
        try {
            return salonId == null ? null : salonRepository.findById(Long.valueOf(salonId)).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

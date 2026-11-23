package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

@Service
public class WebhookServicio {

    private static final String EVENTO_PAGO_COMPLETADO = "checkout.session.completed";
    private static final String EVENTO_SUSCRIPCION_ELIMINADA = "customer.subscription.deleted";
    private static final String ESTADO_PAGADO = "paid";
    private static final int CREDITOS_PRO = 300;
    private static final int CREDITOS_PAQUETE = 200;

    private final StripeClient stripeClient;
    private final StripePropiedades propiedades;
    private final EventoStripeRepository eventoRepository;
    private final SalonRepository salonRepository;
    private final Clock reloj;

    public WebhookServicio(StripeClient stripeClient, StripePropiedades propiedades,
            EventoStripeRepository eventoRepository, SalonRepository salonRepository, Clock reloj) {
        this.stripeClient = stripeClient;
        this.propiedades = propiedades;
        this.eventoRepository = eventoRepository;
        this.salonRepository = salonRepository;
        this.reloj = reloj;
    }

    @Transactional
    public WebhookRespuesta procesar(String payload, String firma) {
        Event evento;
        try {
            evento = stripeClient.constructEvent(payload, firma, propiedades.secretoWebhook());
        } catch (SignatureVerificationException e) {
            // throw con causa
            throw new FirmaInvalidaException("Firma de Stripe inválida", e);
        }
        if (eventoRepository.existsByStripeEventId(evento.getId())) {
            return new WebhookRespuesta(true, false);
        }
        aplicar(evento);
        EventoStripe registro = new EventoStripe();
        registro.setStripeEventId(evento.getId());
        registro.setTipo(evento.getType());
        registro.setFechaCreacion(LocalDateTime.now(reloj));
        eventoRepository.save(registro);
        return new WebhookRespuesta(true, true);
    }

    private void aplicar(Event evento) {
        StripeObject objeto = deserializar(evento);
        if (objeto == null) {
            return;
        }
        if (EVENTO_PAGO_COMPLETADO.equals(evento.getType()) && objeto instanceof Session sesion
                && ESTADO_PAGADO.equals(sesion.getPaymentStatus())) {
            aplicarPago(sesion);
        } else if (EVENTO_SUSCRIPCION_ELIMINADA.equals(evento.getType())
                && objeto instanceof Subscription suscripcion) {
            aplicarCancelacion(suscripcion);
        }
    }

    private void aplicarPago(Session sesion) {
        Map<String, String> metadata = sesion.getMetadata();
        if (metadata == null) {
            return;
        }
        Long salonId = parsearId(metadata.get("salonId"));
        TipoCompra tipo = parsearTipo(metadata.get("tipo"));
        if (salonId == null || tipo == null) {
            return;
        }
        Optional<Salon> salonBuscado = salonRepository.findById(salonId);
        if (salonBuscado.isEmpty()) {
            return;
        }
        Salon salon = salonBuscado.get();
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(sesion.getSubscription());
            salon.setCreditosIa(salon.getCreditosIa() + CREDITOS_PRO);
        } else {
            salon.setCreditosIa(salon.getCreditosIa() + CREDITOS_PAQUETE);
        }
        salonRepository.save(salon);
    }

    private void aplicarCancelacion(Subscription suscripcion) {
        salonRepository.findByStripeSuscripcionId(suscripcion.getId()).ifPresent(salon -> {
            salon.setPlan(Plan.GRATIS);
            salon.setStripeSuscripcionId(null);
            salonRepository.save(salon);
        });
    }

    private StripeObject deserializar(Event evento) {
        try {
            return evento.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException e) {
            return null;
        }
    }

    private static Long parsearId(String valor) {
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TipoCompra parsearTipo(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return TipoCompra.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

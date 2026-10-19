package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.PasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesadorEventoStripe {
    private final EventoStripeRepository eventos;
    private final SalonRepository salones;
    private final Clock reloj;
    private final int creditosPro;
    private final int creditosPaquete;

    public ProcesadorEventoStripe(EventoStripeRepository eventos, SalonRepository salones, Clock reloj,
            @Value("${sillon.creditos-pro}") int creditosPro, @Value("${sillon.creditos-paquete}") int creditosPaquete) {
        this.eventos = eventos; this.salones = salones; this.reloj = reloj;
        this.creditosPro = creditosPro; this.creditosPaquete = creditosPaquete;
    }

    @Transactional
    public WebhookResponse procesar(Event evento) {
        if (eventos.existsById(evento.getId())) { return new WebhookResponse(true, false); }
        switch (evento.getType()) {
            case "checkout.session.completed" -> aplicarCompra(leerObjeto(evento, Session.class));
            case "customer.subscription.deleted" -> cancelar(leerObjeto(evento, Subscription.class));
            default -> { }
        }
        eventos.saveAndFlush(new EventoStripe(evento.getId(), evento.getType(), LocalDateTime.now(reloj)));
        return new WebhookResponse(true, true);
    }

    private void aplicarCompra(Session sesion) {
        if (!"paid".equals(sesion.getPaymentStatus())) { return; }
        Map<String, String> metadata = sesion.getMetadata();
        Long salonId;
        TipoCompra tipo;
        try {
            if (metadata == null) { throw new IllegalArgumentException(); }
            salonId = Long.valueOf(metadata.get("salonId"));
            tipo = TipoCompra.valueOf(metadata.get("tipoCompra"));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new PasarelaException("El pago confirmado no contiene metadata válida");
        }
        Salon salon = salones.findByIdParaActualizar(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            if (sesion.getSubscription() == null || sesion.getSubscription().isBlank()) {
                throw new PasarelaException("El pago confirmado no contiene una suscripción válida");
            }
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(sesion.getSubscription());
            salon.setCreditosIa(salon.getCreditosIa() + creditosPro);
        } else {
            salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
        }
        salones.save(salon);
    }

    private void cancelar(Subscription suscripcion) {
        Salon encontrado = salones.findByStripeSuscripcionId(suscripcion.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        Salon salon = salones.findByIdParaActualizar(encontrado.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        if (suscripcion.getId().equals(salon.getStripeSuscripcionId())) {
            salon.setPlan(Plan.GRATIS);
            salones.save(salon);
        }
    }

    private <T extends StripeObject> T leerObjeto(Event evento, Class<T> tipo) {
        var deserializador = evento.getDataObjectDeserializer();
        StripeObject objeto = deserializador.getObject().orElse(null);
        if (objeto == null) {
            try { objeto = deserializador.deserializeUnsafe(); }
            catch (EventDataObjectDeserializationException ex) {
                throw new PasarelaException("No se pudo interpretar el evento de Stripe");
            }
        }
        if (!tipo.isInstance(objeto)) { throw new PasarelaException("El evento de Stripe contiene un objeto no válido"); }
        return tipo.cast(objeto);
    }
}

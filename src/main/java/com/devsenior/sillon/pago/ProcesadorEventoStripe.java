package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

@Service
public class ProcesadorEventoStripe {

    private static final Logger LOG = LoggerFactory.getLogger(ProcesadorEventoStripe.class);

    private final EventoStripeRepository eventos;
    private final SalonRepository salones;
    private final Clock clock;
    private final int creditosPro;
    private final int creditosPaquete;

    public ProcesadorEventoStripe(EventoStripeRepository eventos, SalonRepository salones, Clock clock,
            @Value("${sillon.creditos.pro}") int creditosPro,
            @Value("${sillon.creditos.paquete}") int creditosPaquete) {
        this.eventos = eventos;
        this.salones = salones;
        this.clock = clock;
        this.creditosPro = creditosPro;
        this.creditosPaquete = creditosPaquete;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean procesar(Event evento) {
        if (eventos.existsById(evento.getId())) {
            return false;
        }
        // El INSERT, no un merge, reclama el id incluso ante entregas simultaneas.
        eventos.registrar(evento.getId(), evento.getType(), LocalDateTime.now(clock));
        return switch (evento.getType()) {
            case "checkout.session.completed" -> aplicarCompra(objeto(evento));
            case "customer.subscription.deleted" -> cancelarSuscripcion(objeto(evento));
            default -> false;
        };
    }

    private boolean aplicarCompra(StripeObject objeto) {
        if (!(objeto instanceof Session sesion) || Boolean.TRUE.equals(sesion.getLivemode())) {
            throw datosInvalidos();
        }
        if (!"paid".equals(sesion.getPaymentStatus())) {
            return false;
        }
        Map<String, String> metadata = sesion.getMetadata();
        if (metadata == null || metadata.get("salonId") == null || metadata.get("tipo") == null) {
            throw datosInvalidos();
        }
        long salonId;
        TipoCompra tipo;
        try {
            salonId = Long.parseLong(metadata.get("salonId"));
            tipo = TipoCompra.valueOf(metadata.get("tipo"));
        } catch (IllegalArgumentException ex) {
            throw datosInvalidos();
        }
        if (salonId <= 0 || (tipo == TipoCompra.SUSCRIPCION_PRO
                && (sesion.getSubscription() == null || sesion.getSubscription().isBlank()))) {
            throw datosInvalidos();
        }
        Salon salon = salones.findOneById(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado"));
        int incremento = tipo == TipoCompra.SUSCRIPCION_PRO ? creditosPro : creditosPaquete;
        salon.setCreditosIa(Math.addExact(salon.getCreditosIa(), incremento));
        if (tipo == TipoCompra.SUSCRIPCION_PRO) {
            salon.setPlan(Plan.PRO);
            salon.setStripeSuscripcionId(sesion.getSubscription());
        }
        return true;
    }

    private boolean cancelarSuscripcion(StripeObject objeto) {
        if (!(objeto instanceof Subscription suscripcion) || suscripcion.getId() == null
                || suscripcion.getId().isBlank()) {
            throw datosInvalidos();
        }
        Salon salon = salones.findOneByStripeSuscripcionId(suscripcion.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón de la suscripción no encontrado"));
        salon.setPlan(Plan.GRATIS);
        return true;
    }

    private StripeObject objeto(Event evento) {
        if (evento.getData() == null) {
            throw datosInvalidos();
        }
        var deserializador = evento.getDataObjectDeserializer();
        var seguro = deserializador.getObject();
        if (seguro.isPresent()) {
            return seguro.get();
        }
        // La firma ya fue verificada; solo se omite la comprobacion de version del objeto.
        LOG.warn("Deserializando objeto Stripe con una version de API distinta a la del SDK");
        try {
            return deserializador.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException ex) {
            throw datosInvalidos();
        }
    }

    private static ErrorDePasarelaException datosInvalidos() {
        return new ErrorDePasarelaException("El evento verificado de Stripe no contiene los datos requeridos");
    }
}

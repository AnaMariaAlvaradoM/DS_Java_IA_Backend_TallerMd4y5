
package com.devsenior.sillon.pago;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.salon.Plan;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@Service
public class ServicioWebhookStripe {

    private static final Logger log =
            LoggerFactory.getLogger(ServicioWebhookStripe.class);

    private final EventoStripeRepository eventoRepository;
    private final SalonRepository salonRepository;
    private final ConfiguracionStripe.PropiedadesStripe propiedades;
    private final Clock reloj;

    public ServicioWebhookStripe(
            EventoStripeRepository eventoRepository,
            SalonRepository salonRepository,
            ConfiguracionStripe.PropiedadesStripe propiedades,
            Clock reloj
    ) {
        this.eventoRepository = eventoRepository;
        this.salonRepository = salonRepository;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Transactional
    public boolean procesar(String cuerpo, String firma) {

        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException(
                    "Falta la firma de Stripe"
            );
        }

        Event evento;

        try {
            evento = Webhook.constructEvent(
                    cuerpo,
                    firma,
                    propiedades.secretoWebhook()
            );
        
        } catch (SignatureVerificationException ex) {
            throw new FirmaInvalidaException(
            "La firma de Stripe no es válida"
            );
        } catch (RuntimeException ex) {
            throw new FirmaInvalidaException(
            "El evento de Stripe contiene datos inválidos"
            );
        }


        if (eventoRepository.existsById(evento.getId())) {
            return false;
        }

        switch (evento.getType()) {
            case "checkout.session.completed" ->
                    procesarCompra(evento);

            case "customer.subscription.deleted" ->
                    procesarCancelacion(evento);

            default -> {
                // Se registra el evento sin modificar el salón.
            }
        }

        eventoRepository.saveAndFlush(
                new EventoStripe(
                        evento.getId(),
                        evento.getType(),
                        LocalDateTime.now(reloj)
                )
        );

        return true;
    }

    private void procesarCompra(Event evento) {

        StripeObject objeto = obtenerObjeto(evento);

        if (!(objeto instanceof Session sesion)) {
            throw new IllegalStateException(
                    "El evento no contiene una sesión de Checkout"
            );
        }

        if (!"paid".equals(sesion.getPaymentStatus())) {
            return;
        }

        Map<String, String> metadata = sesion.getMetadata();

        if (metadata == null
                || !metadata.containsKey("salonId")
                || !metadata.containsKey("tipoCompra")) {
            throw new IllegalStateException(
                    "Faltan metadatos de la compra"
            );
        }

        Long salonId = Long.valueOf(metadata.get("salonId"));

        TipoCompra tipo = TipoCompra.valueOf(
                metadata.get("tipoCompra")
        );

        Salon salon = salonRepository.findByIdParaAgendar(salonId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Salón no encontrado"
                ));

        switch (tipo) {
            case SUSCRIPCION_PRO -> {

                if (sesion.getSubscription() == null) {
                    throw new IllegalStateException(
                            "Falta el identificador de la suscripción"
                    );
                }

                salon.setPlan(Plan.PRO);
                salon.setCreditosIa(
                        salon.getCreditosIa() + 300
                );
                salon.setStripeSuscripcionId(
                        sesion.getSubscription()
                );
            }

            case PAQUETE_CREDITOS ->
                    salon.setCreditosIa(
                            salon.getCreditosIa() + 200
                    );
        }

        salonRepository.save(salon);
    }

    private void procesarCancelacion(Event evento) {

        StripeObject objeto = obtenerObjeto(evento);

        if (!(objeto instanceof Subscription suscripcion)) {
            throw new IllegalStateException(
                    "El evento no contiene una suscripción"
            );
        }

        salonRepository
                .findByStripeSuscripcionId(suscripcion.getId())
                .ifPresent(salon -> {
                    salon.setPlan(Plan.GRATIS);
                    salon.setStripeSuscripcionId(null);
                    salonRepository.save(salon);
                });
    }

    private StripeObject obtenerObjeto(Event evento) {

        EventDataObjectDeserializer deserializador =
                evento.getDataObjectDeserializer();

        var objeto = deserializador.getObject();

        if (objeto.isPresent()) {
            return objeto.get();
        }

        log.warn(
                "El evento {} requiere deserialización sin comprobación de versión",
                evento.getId()
        );

        try {
            return deserializador.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException ex) {
            throw new IllegalStateException(
                    "No fue posible leer el evento de Stripe",
                    ex
            );
        }
    }
}

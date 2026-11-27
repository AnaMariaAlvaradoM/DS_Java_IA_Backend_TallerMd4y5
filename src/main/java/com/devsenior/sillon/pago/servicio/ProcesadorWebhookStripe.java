
package com.devsenior.sillon.pago.servicio;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.modelo.EventoStripe;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devsenior.sillon.salon.modelo.Plan;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

@Service
public class ProcesadorWebhookStripe {

    private final EventoStripeRepositorio eventoRepositorio;
    private final SalonRepositorio salonRepositorio;
    private final Clock reloj;

    public ProcesadorWebhookStripe(
            EventoStripeRepositorio eventoRepositorio,
            SalonRepositorio salonRepositorio,
            Clock reloj) {

        this.eventoRepositorio = eventoRepositorio;
        this.salonRepositorio = salonRepositorio;
        this.reloj = reloj;
    }

    @Transactional
    public RespuestaWebhook procesar(Event evento) {

        if (evento == null
                || evento.getId() == null
                || evento.getId().isBlank()) {
            throw new ExcepcionIntegracion(
                    "El evento de Stripe no tiene identificador");
        }

        String tipoEvento = evento.getType();

        if (tipoEvento == null || tipoEvento.isBlank()) {
            throw new ExcepcionIntegracion(
                    "El evento de Stripe no tiene tipo");
        }

        boolean procesado;

        switch (tipoEvento) {
            case "checkout.session.completed" ->
                procesado = procesarSesion(evento);

            case "customer.subscription.deleted" ->
                procesado = procesarCancelacion(evento);

            default -> procesado = false;
        }

        return new RespuestaWebhook(true, procesado);
    }

    private boolean procesarSesion(Event evento) {

        StripeObject objeto = extraerObjeto(evento);

        if (!(objeto instanceof Session sesion)) {
            throw new ExcepcionIntegracion(
                    "El evento no contiene una sesión de Stripe válida");
        }

        if (!"paid".equals(sesion.getPaymentStatus())) {
            return false;
        }

        Map<String, String> metadata = sesion.getMetadata();

        if (metadata == null) {
            throw new ExcepcionIntegracion(
                    "La sesión no contiene metadata");
        }

        Long salonId = obtenerSalonId(
                metadata,
                sesion.getClientReferenceId());

        TipoCompra tipoCompra;

        String valorTipo = metadata.get("tipoCompra");

        if (valorTipo == null || valorTipo.isBlank()) {
            valorTipo = metadata.get("tipo");
        }

        try {
            tipoCompra = TipoCompra.valueOf(valorTipo);
        } catch (IllegalArgumentException
                | NullPointerException excepcion) {
            throw new ExcepcionIntegracion(
                    "La sesión contiene un tipo de compra inválido");
        }

        // Primer acceso al salón: bloqueo de escritura.
        // Los webhooks del mismo salón se serializan aquí.
        Salon salon = salonRepositorio
                .buscarBloqueadoPorId(salonId)
                .orElseThrow(
                        () -> new ExcepcionRecursoNoEncontrado(
                                "Salón no encontrado"));

        // Comprobar el evento DESPUÉS de obtener el bloqueo.
        // Evita utilizar una lectura anterior al bloqueo
        // bajo el aislamiento REPEATABLE_READ de MySQL.
        if (eventoRepositorio.existsById(evento.getId())) {
            return false;
        }

        switch (tipoCompra) {
            case SUSCRIPCION_PRO -> {

                String suscripcionId = sesion.getSubscription();

                if (suscripcionId == null
                        || suscripcionId.isBlank()) {
                    throw new ExcepcionIntegracion(
                            "La sesión PRO no contiene una suscripción");
                }

                if (salon.getPlan() == Plan.PRO) {
                    return false;
                }

                salon.setPlan(Plan.PRO);
                salon.setStripeSuscripcionId(
                        suscripcionId);
                salon.setCreditosIa(
                        salon.getCreditosIa() + 300);
            }

            case PAQUETE_CREDITOS -> salon.setCreditosIa(
                    salon.getCreditosIa() + 200);
        }

        salonRepositorio.save(salon);
        registrarEvento(evento);

        return true;
    }

    private boolean procesarCancelacion(Event evento) {

        StripeObject objeto = extraerObjeto(evento);

        if (!(objeto instanceof Subscription suscripcion)) {
            throw new ExcepcionIntegracion(
                    "El evento no contiene una suscripción válida");
        }

        String suscripcionId = suscripcion.getId();

        if (suscripcionId == null
                || suscripcionId.isBlank()) {
            throw new ExcepcionIntegracion(
                    "La cancelación no tiene identificador de suscripción");
        }

        // Bloquear el salón asociado a la suscripción.
        // El mismo salón no debe modificarse en paralelo.
        var salonEncontrado = salonRepositorio
                .buscarBloqueadoPorSuscripcionId(
                        suscripcionId);

        if (salonEncontrado.isEmpty()) {
            return false;
        }

        Salon salon = salonEncontrado.get();

        // Segunda comprobación dentro de la transacción,
        // después de adquirir el bloqueo.
        if (eventoRepositorio.existsById(evento.getId())) {
            return false;
        }

        salon.setPlan(Plan.GRATIS);
        salon.setStripeSuscripcionId(null);

        // Los créditos existentes se conservan.
        salonRepositorio.save(salon);
        registrarEvento(evento);

        return true;
    }

    private void registrarEvento(Event evento) {

        eventoRepositorio.saveAndFlush(
                new EventoStripe(
                        evento.getId(),
                        evento.getType(),
                        LocalDateTime.now(reloj)));
    }

    private StripeObject extraerObjeto(Event evento) {

        var deserializador = evento.getDataObjectDeserializer();

        if (deserializador == null) {
            throw new ExcepcionIntegracion(
                    "El evento no contiene datos para procesar");
        }

        var objeto = deserializador.getObject();

        if (objeto.isPresent()) {
            return objeto.get();
        }

        try {
            return deserializador.deserializeUnsafe();
        } catch (EventDataObjectDeserializationException excepcion) {
            throw new ExcepcionIntegracion(
                    "No fue posible interpretar el evento de Stripe");
        }
    }

    private Long obtenerSalonId(
            Map<String, String> metadata,
            String referencia) {

        String valor = metadata.get("salonId");

        if (valor == null || valor.isBlank()) {
            valor = referencia;
        }

        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException
                | NullPointerException excepcion) {
            throw new ExcepcionIntegracion(
                    "La sesión contiene un identificador de salón inválido");
        }
    }
}

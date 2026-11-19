package com.devsenior.sillon.pago;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.devsenior.sillon.compartido.excepcion.ErrorDePasarelaException;
import com.devsenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Service
public class WebhookStripeService {

    private final ProcesadorEventoStripe procesador;
    private final EventoStripeRepository eventos;
    private final Clock clock;
    private final String secreto;

    public WebhookStripeService(ProcesadorEventoStripe procesador, EventoStripeRepository eventos, Clock clock,
            @Value("${sillon.stripe.secreto-webhook}") String secreto) {
        this.procesador = procesador;
        this.eventos = eventos;
        this.clock = clock;
        this.secreto = secreto;
    }

    public WebhookResponse recibir(String cuerpo, String firma) {
        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException("Falta la firma de Stripe");
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(cuerpo, firma, secreto, Webhook.DEFAULT_TOLERANCE, clock);
        } catch (SignatureVerificationException ex) {
            throw new FirmaInvalidaException("La firma de Stripe no es válida");
        }
        if (evento == null || evento.getId() == null || evento.getId().isBlank()
                || evento.getType() == null || evento.getType().isBlank()
                || Boolean.TRUE.equals(evento.getLivemode())) {
            throw new ErrorDePasarelaException("Stripe no envió un evento de prueba válido");
        }
        boolean procesado;
        try {
            procesado = procesador.procesar(evento);
        } catch (DataIntegrityViolationException ex) {
            // La transaccion del procesador ya termino y revirtio antes de comprobar el duplicado.
            if (!eventos.existsById(evento.getId())) {
                throw ex;
            }
            procesado = false;
        }
        return new WebhookResponse(true, procesado);
    }
}

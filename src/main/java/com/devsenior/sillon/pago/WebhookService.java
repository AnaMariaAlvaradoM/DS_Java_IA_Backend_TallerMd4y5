package com.devsenior.sillon.pago;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {
    private final VerificadorWebhook verificador;
    private final ProcesadorEventoStripe procesador;
    private final EventoStripeRepository eventos;
    public WebhookService(VerificadorWebhook verificador, ProcesadorEventoStripe procesador, EventoStripeRepository eventos) {
        this.verificador = verificador; this.procesador = procesador; this.eventos = eventos;
    }

    public WebhookResponse recibir(String cuerpo, String firma) {
        var evento = verificador.verificar(cuerpo, firma);
        try { return procesador.procesar(evento); }
        catch (DataIntegrityViolationException ex) {
            if (eventos.existsById(evento.getId())) { return new WebhookResponse(true, false); }
            throw ex;
        }
    }
}


package com.devsenior.sillon.pago.servicio;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.compartido.excepcion.ExcepcionSolicitudInvalida;
import com.devsenior.sillon.pago.configuracion.PropiedadesStripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Component
public class ValidadorWebhookStripe {

    private final PropiedadesStripe propiedades;

    public ValidadorWebhookStripe(PropiedadesStripe propiedades) {
        this.propiedades = propiedades;
    }

    public Event validar(String cuerpo, String firma) {
        if (firma == null || firma.isBlank()
                || cuerpo == null || cuerpo.isBlank()) {
            throw new ExcepcionSolicitudInvalida(
                    "El webhook requiere cuerpo y firma válidos");
        }

        try {
            return Webhook.constructEvent(
                    cuerpo,
                    firma,
                    propiedades.secretoWebhook());
        } catch (SignatureVerificationException excepcion) {
            throw new ExcepcionSolicitudInvalida(
                    "La firma del webhook de Stripe no es válida");
        } catch (IllegalArgumentException excepcion) {
            throw new ExcepcionSolicitudInvalida(
                    "El evento de Stripe no tiene un formato válido");
        }
    }
}

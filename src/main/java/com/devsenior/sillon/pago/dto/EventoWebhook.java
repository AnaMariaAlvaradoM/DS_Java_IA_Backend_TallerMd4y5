package com.devsenior.sillon.pago.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Solo los campos del evento de Stripe que Sillón necesita, leídos con Jackson.
 * No se usan los modelos de stripe-java: si el api_version del evento no coincide con el de la librería,
 * el objeto llegaría vacío.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoWebhook(String id, String type, Datos data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Datos(Objeto object) {
    }

    /** checkout.session (pago) o subscription (cancelación). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Objeto(
            String id,
            @JsonProperty("payment_status") String estadoPago,
            String subscription,
            Map<String, String> metadata
    ) {
    }

    public Objeto objeto() {
        return data == null ? null : data.object();
    }
}

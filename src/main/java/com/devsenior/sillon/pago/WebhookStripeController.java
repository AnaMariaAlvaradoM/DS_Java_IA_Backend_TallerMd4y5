package com.devsenior.sillon.pago;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.pago.dto.WebhookResponse;

@RestController
@RequestMapping("/api/pagos")
public class WebhookStripeController {

    private final WebhookStripeService webhook;

    public WebhookStripeController(WebhookStripeService webhook) {
        this.webhook = webhook;
    }

    @PostMapping("/webhook")
    public WebhookResponse recibir(@RequestBody String cuerpo,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return webhook.recibir(cuerpo, firma);
    }
}

package com.devsenior.sillon.pago;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {
    private final PagoService pagos;
    private final WebhookService webhook;

    public PagoController(PagoService pagos, WebhookService webhook) {
        this.pagos = pagos;
        this.webhook = webhook;
    }

    @PostMapping("/suscripcion")
    public SesionPagoResponse suscripcion(@AuthenticationPrincipal UsuarioPrincipal dueno) {
        return pagos.crearSuscripcion(dueno);
    }

    @PostMapping("/creditos")
    public SesionPagoResponse creditos(@AuthenticationPrincipal UsuarioPrincipal dueno) {
        return pagos.comprarCreditos(dueno);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WebhookRespuesta webhook(@RequestBody String cuerpo,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return webhook.procesar(cuerpo, firma);
    }

    @GetMapping("/resultado")
    public MensajeResultado resultado(@RequestParam String estado) {
        return new MensajeResultado("exito".equals(estado)
                ? "Pago completado. Estamos confirmando la operación."
                : "Pago cancelado.");
    }

    public record MensajeResultado(String mensaje) {
    }
}

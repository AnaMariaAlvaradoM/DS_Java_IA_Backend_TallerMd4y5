package com.devsenior.sillon.pago;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;
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
public class PagoControlador {
    private final PagoServicio pagos;
    private final VerificadorWebhook verificador;
    private final WebhookServicio webhook;

    public PagoControlador(PagoServicio pagos, VerificadorWebhook verificador, WebhookServicio webhook) {
        this.pagos = pagos;
        this.verificador = verificador;
        this.webhook = webhook;
    }

    @PostMapping("/suscripcion")
    public SesionPagoRespuesta suscripcion(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagos.suscripcion(usuario);
    }

    @PostMapping("/creditos")
    public SesionPagoRespuesta creditos(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagos.creditos(usuario);
    }

    @PostMapping("/webhook")
    public WebhookRespuesta webhook(@RequestBody String cuerpo,
            @RequestHeader(name = "Stripe-Signature", required = false) String firma) {
        return webhook.procesar(verificador.verificar(cuerpo, firma));
    }

    @GetMapping("/resultado")
    public ResultadoPagoRespuesta resultado(@RequestParam String estado) { return pagos.resultado(estado); }
}

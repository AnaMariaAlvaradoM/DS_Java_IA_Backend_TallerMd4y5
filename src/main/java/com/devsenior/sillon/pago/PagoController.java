package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.excepcion.DatosInvalidosException;
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
public class PagoController {
    private final PagoService pagos;
    private final WebhookService webhook;
    public PagoController(PagoService pagos, WebhookService webhook) { this.pagos = pagos; this.webhook = webhook; }

    @PostMapping("/suscripcion")
    public PagoResponse suscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagos.crear(principal, TipoCompra.SUSCRIPCION_PRO);
    }
    @PostMapping("/creditos")
    public PagoResponse creditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagos.crear(principal, TipoCompra.PAQUETE_CREDITOS);
    }
    @PostMapping("/webhook")
    public WebhookResponse webhook(@RequestBody String cuerpo,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return webhook.recibir(cuerpo, firma);
    }
    @GetMapping("/resultado")
    public ResultadoPagoResponse resultado(@RequestParam String estado) {
        return switch (estado) {
            case "exito" -> new ResultadoPagoResponse("Regresó del pago. Stripe confirmará su estado mediante el webhook.");
            case "cancelado" -> new ResultadoPagoResponse("El pago fue cancelado.");
            default -> throw new DatosInvalidosException("estado: debe ser exito o cancelado");
        };
    }
    public record ResultadoPagoResponse(String mensaje) { }
}

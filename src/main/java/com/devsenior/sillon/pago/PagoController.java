package com.devsenior.sillon.pago;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/suscripcion")
    public SesionPagoRespuesta crearSuscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagoService.crearSesion(principal, TipoCompra.SUSCRIPCION_PRO);
    }

    @PostMapping("/creditos")
    public SesionPagoRespuesta crearCreditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagoService.crearSesion(principal, TipoCompra.PAQUETE_CREDITOS);
    }

    @PostMapping("/webhook")
    public WebhookRespuesta webhook(@RequestBody String cuerpo,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return pagoService.procesarWebhook(cuerpo, firma);
    }

    @GetMapping("/resultado")
    public ResultadoPagoRespuesta resultado(@RequestParam String estado) {
        String mensaje = switch (estado) {
            case "exito" -> "El pago se realizo correctamente.";
            case "cancelado" -> "El pago fue cancelado.";
            default -> "Resultado de pago desconocido.";
        };
        return new ResultadoPagoRespuesta(mensaje);
    }
}

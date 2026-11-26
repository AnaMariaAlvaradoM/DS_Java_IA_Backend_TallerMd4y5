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
    public PagoRespuesta suscripcion(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagoService.crearSesion(usuario.getSalonId(), usuario.getUsername(), TipoCompra.SUSCRIPCION_PRO);
    }

    @PostMapping("/creditos")
    public PagoRespuesta creditos(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagoService.crearSesion(usuario.getSalonId(), usuario.getUsername(), TipoCompra.PAQUETE_CREDITOS);
    }

    /** El cuerpo llega como texto plano: la firma se calcula sobre los bytes exactos. */
    @PostMapping("/webhook")
    public WebhookRespuesta webhook(@RequestBody(required = false) String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return pagoService.procesarWebhook(payload, firma);
    }

    /** Página a la que vuelve Stripe después del pago. */
    @GetMapping("/resultado")
    public MensajeRespuesta resultado(@RequestParam(required = false) String estado) {
        if ("exito".equalsIgnoreCase(estado)) {
            return new MensajeRespuesta("¡Pago recibido! En unos segundos tu salón quedará actualizado.");
        }
        if ("cancelado".equalsIgnoreCase(estado)) {
            return new MensajeRespuesta("Cancelaste el pago. No se hizo ningún cobro.");
        }
        return new MensajeRespuesta("Estado de pago desconocido.");
    }
}

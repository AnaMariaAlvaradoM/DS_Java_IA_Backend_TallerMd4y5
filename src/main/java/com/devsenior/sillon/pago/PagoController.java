package com.devsenior.sillon.pago;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.compartido.FirmaInvalidaException;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoServicio pagoServicio;
    private final WebhookServicio webhookServicio;

    public PagoController(PagoServicio pagoServicio, WebhookServicio webhookServicio) {
        this.pagoServicio = pagoServicio;
        this.webhookServicio = webhookServicio;
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<CheckoutRespuesta> suscripcion(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoServicio.crearSuscripcion(principal));
    }

    @PostMapping("/creditos")
    public ResponseEntity<CheckoutRespuesta> creditos(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(pagoServicio.crearCreditos(principal));
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookRespuesta> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        if (firma == null || firma.isBlank()) {
            throw new FirmaInvalidaException("Falta la cabecera Stripe-Signature");
        }
        return ResponseEntity.ok(webhookServicio.procesar(payload, firma));
    }

    @GetMapping("/resultado")
    public ResponseEntity<ResultadoRespuesta> resultado(@RequestParam String estado) {
        return ResponseEntity.ok(new ResultadoRespuesta(pagoServicio.mensajeResultado(estado)));
    }
}

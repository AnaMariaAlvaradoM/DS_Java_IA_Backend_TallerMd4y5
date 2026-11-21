package com.devsenior.sillon.pago;

import com.devsenior.sillon.pago.dto.ResultadoPagoRespuesta;
import com.devsenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devsenior.sillon.pago.dto.WebhookRespuesta;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<SesionPagoRespuesta> crearSuscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
        SesionPagoRespuesta respuesta = pagoService.crearSesionSuscripcion(principal);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/creditos")
    public ResponseEntity<SesionPagoRespuesta> crearCreditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        SesionPagoRespuesta respuesta = pagoService.crearSesionCreditos(principal);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookRespuesta> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader
    ) {
        WebhookRespuesta respuesta = pagoService.procesarWebhook(payload, sigHeader);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/resultado")
    public ResponseEntity<ResultadoPagoRespuesta> resultado(@RequestParam(defaultValue = "exito") String estado) {
        ResultadoPagoRespuesta respuesta = pagoService.obtenerResultado(estado);
        return ResponseEntity.ok(respuesta);
    }
}

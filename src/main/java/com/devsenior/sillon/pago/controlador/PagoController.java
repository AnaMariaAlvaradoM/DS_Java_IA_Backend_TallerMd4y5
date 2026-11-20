package com.devsenior.sillon.pago.controlador;

import com.devsenior.sillon.pago.dto.SesionPagoResponse;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.servicio.PagoService;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<SesionPagoResponse> crearSuscripcion(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(pagoService.crearSesionPago(usuario, TipoCompra.SUSCRIPCION_PRO));
    }

    @PostMapping("/creditos")
    public ResponseEntity<SesionPagoResponse> comprarCreditos(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return ResponseEntity.ok(pagoService.crearSesionPago(usuario, TipoCompra.PAQUETE_CREDITOS));
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookResponse> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return ResponseEntity.ok(pagoService.procesarWebhook(payload, signature));
    }

    @GetMapping("/resultado")
    public ResponseEntity<Map<String, String>> resultado(@RequestParam(defaultValue = "exito") String estado) {
        String mensaje = "exito".equalsIgnoreCase(estado)
                ? "Pago procesado con éxito. Ya puedes volver a Sillón."
                : "El pago fue cancelado. No se realizó ningún cobro.";
        return ResponseEntity.ok(Map.of("estado", estado, "mensaje", mensaje));
    }
}
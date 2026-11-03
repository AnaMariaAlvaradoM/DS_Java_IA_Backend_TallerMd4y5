package com.devsenior.sillon.pago;

import com.devsenior.sillon.compartido.SolicitudInvalidaExcepcion;
import com.devsenior.sillon.pago.dto.PagoRespuestaDto;
import com.devsenior.sillon.pago.dto.WebhookRespuestaDto;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<PagoRespuestaDto> crearSesionSuscripcion(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        PagoRespuestaDto respuesta = pagoService.crearSesion(
                principal.getSalonId(),
                principal.getUsername(),
                TipoCompra.SUSCRIPCION_PRO
        );
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/creditos")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<PagoRespuestaDto> crearSesionCreditos(
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        PagoRespuestaDto respuesta = pagoService.crearSesion(
                principal.getSalonId(),
                principal.getUsername(),
                TipoCompra.PAQUETE_CREDITOS
        );
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/resultado")
    public ResponseEntity<String> resultadoPago(@RequestParam String estado) {
        if ("exito".equalsIgnoreCase(estado)) {
            return ResponseEntity.ok("Pago procesado exitosamente. ¡Gracias por tu compra!");
        } else if ("cancelado".equalsIgnoreCase(estado)) {
            return ResponseEntity.ok("El proceso de pago fue cancelado.");
        } else {
            throw new SolicitudInvalidaExcepcion("Parámetro de estado inválido");
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<WebhookRespuestaDto> recibirWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String firma) {

        WebhookRespuestaDto respuesta = pagoService.procesarWebhook(payload, firma);
        return ResponseEntity.ok(respuesta);
    }
}

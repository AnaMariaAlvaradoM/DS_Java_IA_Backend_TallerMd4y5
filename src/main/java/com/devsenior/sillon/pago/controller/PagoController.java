package com.devsenior.sillon.pago.controller;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.pago.dto.PagoResponse;
import com.devsenior.sillon.pago.service.PagoService;
import com.devsenior.sillon.pago.service.PagoWebhookService;
import com.devsenior.sillon.security.UsuarioPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final PagoWebhookService pagoWebhookService;

    public PagoController(PagoService pagoService, PagoWebhookService pagoWebhookService) {
        this.pagoService = pagoService;
        this.pagoWebhookService = pagoWebhookService;
    }

    // 1. POST /api/pagos/suscripcion [DUENO]
    @PostMapping("/suscripcion")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<PagoResponse> crearSuscripcion(Authentication auth) {
        // 1. Convertir el principal genérico a tu clase UsuarioPrincipal
        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();
        if (usuario.getSalonId() == null) {
            throw new ReglaNegocioException("El usuario autenticado no tiene un salón asociado.");
        }
        String email = usuario.getUsername();
        Long salonId = usuario.getSalonId();

        // 3. Llamar al servicio pasando los datos extraídos
        PagoResponse response = pagoService.crearSesionSuscripcion(email, salonId);
        return ResponseEntity.ok(response);
    }

    // 2. POST /api/pagos/creditos [DUENO]
    @PostMapping("/creditos")
    @PreAuthorize("hasRole('DUENO')")
    public ResponseEntity<PagoResponse> crearCreditos(Authentication auth) {
        UsuarioPrincipal usuario = (UsuarioPrincipal) auth.getPrincipal();
        String email = usuario.getUsername();
        Long salonId = usuario.getSalonId();

        PagoResponse response = pagoService.crearSesionCreditos(email, salonId);
        return ResponseEntity.ok(response);
    }

    // 3. POST /api/pagos/webhook [Stripe]
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> procesarWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {

        Map<String, Boolean> respuesta = pagoWebhookService.procesarEvento(payload, signature);
        return ResponseEntity.ok(respuesta); // 200 OK
    }


    // 4. GET /api/pagos/resultado?estado= [público]
    @GetMapping("/resultado")
    public ResponseEntity<Map<String, String>> mostrarResultadoPago(
            @RequestParam(name = "estado", defaultValue = "exito") String estado) {

        if ("exito".equalsIgnoreCase(estado)) {
            return ResponseEntity.ok(Map.of(
                    "estado", "exito",
                    "mensaje", "El pago se ha completado correctamente."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "estado", "cancelado",
                "mensaje", "El proceso de pago fue cancelado por el usuario."
        ));
    }
}

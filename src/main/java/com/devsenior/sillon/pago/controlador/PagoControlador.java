
package com.devsenior.sillon.pago.controlador;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.pago.dto.RespuestaSesionPago;
import com.devsenior.sillon.pago.dto.RespuestaWebhook;
import com.devsenior.sillon.pago.modelo.TipoCompra;
import com.devsenior.sillon.pago.servicio.PagoServicio;
import com.devsenior.sillon.pago.servicio.ProcesadorWebhookStripe;
import com.devsenior.sillon.pago.servicio.ValidadorWebhookStripe;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/pagos")
public class PagoControlador {

    private final PagoServicio pagoServicio;
    private final ValidadorWebhookStripe validadorWebhook;
    private final ProcesadorWebhookStripe procesadorWebhook;

    public PagoControlador(
            PagoServicio pagoServicio,
            ValidadorWebhookStripe validadorWebhook,
            ProcesadorWebhookStripe procesadorWebhook) {

        this.pagoServicio = pagoServicio;
        this.validadorWebhook = validadorWebhook;
        this.procesadorWebhook = procesadorWebhook;
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<RespuestaSesionPago> suscripcion(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        return ResponseEntity.ok(pagoServicio.crearSesion(
                usuario.getSalonId(),
                usuario.getUsername(),
                TipoCompra.SUSCRIPCION_PRO));
    }

    @PostMapping("/creditos")
    public ResponseEntity<RespuestaSesionPago> creditos(
            @AuthenticationPrincipal UsuarioPrincipal usuario) {

        return ResponseEntity.ok(pagoServicio.crearSesion(
                usuario.getSalonId(),
                usuario.getUsername(),
                TipoCompra.PAQUETE_CREDITOS));
    }

    @PostMapping("/webhook")
    public ResponseEntity<RespuestaWebhook> webhook(
            @RequestBody String cuerpo,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false) String firma) {

        var evento = validadorWebhook.validar(cuerpo, firma);

        return ResponseEntity.ok(
                procesadorWebhook.procesar(evento));
    }

    @GetMapping("/resultado")
    public ResponseEntity<Map<String, String>> resultado(
            @RequestParam String estado) {

        String mensaje = "exito".equals(estado)
                ? "El proceso de pago ha finalizado."
                : "El proceso de pago fue cancelado.";

        return ResponseEntity.ok(Map.of("mensaje", mensaje));
    }
}

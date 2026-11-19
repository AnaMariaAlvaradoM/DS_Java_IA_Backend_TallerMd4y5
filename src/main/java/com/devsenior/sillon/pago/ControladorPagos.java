
package com.devsenior.sillon.pago;

import java.util.Map;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import org.springframework.http.ResponseEntity;
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
public class ControladorPagos {

    private final ServicioPagos servicioPagos;
    private final ServicioWebhookStripe servicioWebhookStripe;

    public ControladorPagos(
            ServicioPagos servicioPagos,
            ServicioWebhookStripe servicioWebhookStripe
    ) {
        this.servicioPagos = servicioPagos;
        this.servicioWebhookStripe = servicioWebhookStripe;
    }

    @PostMapping("/suscripcion")
    public ResponseEntity<RespuestaCheckout> crearSuscripcion(
            @AuthenticationPrincipal UsuarioPrincipal usuario
    ) {
        RespuestaCheckout respuesta = servicioPagos.crearSesion(
                usuario.getSalonId(),
                usuario.getUsername(),
                TipoCompra.SUSCRIPCION_PRO
        );

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/creditos")
    public ResponseEntity<RespuestaCheckout> comprarCreditos(
            @AuthenticationPrincipal UsuarioPrincipal usuario
    ) {
        RespuestaCheckout respuesta = servicioPagos.crearSesion(
                usuario.getSalonId(),
                usuario.getUsername(),
                TipoCompra.PAQUETE_CREDITOS
        );

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Boolean>> recibirWebhook(
            @RequestBody String cuerpo,
            @RequestHeader(
                    value = "Stripe-Signature",
                    required = false
            ) String firma
    ) {
        boolean procesado = servicioWebhookStripe.procesar(
                cuerpo,
                firma
        );

        return ResponseEntity.ok(Map.of(
                "recibido", true,
                "procesado", procesado
        ));
    }

    @GetMapping("/resultado")
    public ResponseEntity<String> consultarResultado(
            @RequestParam String estado
    ) {
        String mensaje = switch (estado) {
            case "exito" ->
                    "Pago completado. Estamos verificando la confirmación de Stripe.";
            case "cancelado" ->
                    "El proceso de pago fue cancelado.";
            default ->
                    "Estado del pago no reconocido.";
        };

        return ResponseEntity.ok(mensaje);
    }
}

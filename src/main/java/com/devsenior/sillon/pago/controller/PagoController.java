package com.devsenior.sillon.pago.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.devsenior.sillon.pago.dto.PagoResponse;
import com.devsenior.sillon.pago.dto.ResultadoPagoResponse;
import com.devsenior.sillon.pago.dto.WebhookResponse;
import com.devsenior.sillon.pago.model.TipoCompra;
import com.devsenior.sillon.pago.service.PagoService;
import com.devsenior.sillon.pago.service.WebhookService;
import com.devsenior.sillon.seguridad.model.UsuarioPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "7 Pagos", description = "Plan PRO y paquete de créditos con Stripe (modo prueba)")
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final WebhookService webhookService;

    public PagoController(PagoService pagoService, WebhookService webhookService) {
        this.pagoService = pagoService;
        this.webhookService = webhookService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/suscripcion")
    public PagoResponse suscripcion(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagoService.crearSesion(usuario.getSalonId(), usuario.getUsername(), TipoCompra.SUSCRIPCION_PRO);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/creditos")
    public PagoResponse creditos(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return pagoService.crearSesion(usuario.getSalonId(), usuario.getUsername(), TipoCompra.PAQUETE_CREDITOS);
    }

    /** El cuerpo llega como texto crudo: convertirlo a DTO antes de verificar la firma cambiaría los bytes. */
    @PostMapping("/webhook")
    public WebhookResponse webhook(@RequestBody String cuerpo,
                                   @RequestHeader(value = "Stripe-Signature", required = false) String firma) {
        return webhookService.procesar(cuerpo, firma);
    }

    /** Página a la que vuelve Stripe. El plan NO se activa aquí: lo activa el webhook. */
    @GetMapping("/resultado")
    public ResultadoPagoResponse resultado(@RequestParam String estado) {
        String mensaje = "exito".equalsIgnoreCase(estado)
                ? "Pago recibido. Su plan se actualizará en cuanto Stripe confirme el pago."
                : "El pago fue cancelado. Puede intentarlo de nuevo cuando quiera.";
        return new ResultadoPagoResponse(estado, mensaje);
    }
}

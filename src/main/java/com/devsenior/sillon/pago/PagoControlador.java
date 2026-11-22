package com.devsenior.sillon.pago;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.seguridad.UsuarioPrincipal;

@RestController
@RequestMapping("/api/pagos")
public class PagoControlador {

    private final PagoServicio pagoServicio;

    public PagoControlador(PagoServicio pagoServicio) {
        this.pagoServicio = pagoServicio;
    }

    @PostMapping("/suscripcion")
    @ResponseStatus(HttpStatus.OK)
    public SesionPagoRespuesta crearSesionSuscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagoServicio.crearSesionSuscripcion(principal.getSalonId(), principal.getUsername());
    }

    @PostMapping("/creditos")
    @ResponseStatus(HttpStatus.OK)
    public SesionPagoRespuesta crearSesionCreditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagoServicio.crearSesionCreditos(principal.getSalonId(), principal.getUsername());
    }

    @PostMapping(value = "/webhook", consumes = {MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE, "*/*"})
    @ResponseStatus(HttpStatus.OK)
    public WebhookRespuesta webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        return pagoServicio.procesarWebhook(payload, sigHeader);
    }

    @GetMapping("/resultado")
    @ResponseStatus(HttpStatus.OK)
    public ResultadoPagoRespuesta resultadoPago(@RequestParam(value = "estado", required = false) String estado) {
        return pagoServicio.obtenerResultado(estado);
    }
}

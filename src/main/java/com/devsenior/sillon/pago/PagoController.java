package com.devsenior.sillon.pago;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.sillon.pago.dto.ResultadoPagoRequest;
import com.devsenior.sillon.pago.dto.ResultadoPagoResponse;
import com.devsenior.sillon.pago.dto.SesionPagoResponse;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagos;

    public PagoController(PagoService pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/suscripcion")
    public SesionPagoResponse suscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagos.crearSesion(principal, TipoCompra.SUSCRIPCION_PRO);
    }

    @PostMapping("/creditos")
    public SesionPagoResponse creditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return pagos.crearSesion(principal, TipoCompra.PAQUETE_CREDITOS);
    }

    @GetMapping("/resultado")
    public ResultadoPagoResponse resultado(@Valid @ModelAttribute ResultadoPagoRequest solicitud) {
        String mensaje = solicitud.estado().equals("exito")
                ? "Pago enviado para confirmación. Los beneficios dependen del webhook verificado de Stripe."
                : "Proceso de pago cancelado. Esta página no modifica los beneficios del salón.";
        return new ResultadoPagoResponse(mensaje);
    }
}

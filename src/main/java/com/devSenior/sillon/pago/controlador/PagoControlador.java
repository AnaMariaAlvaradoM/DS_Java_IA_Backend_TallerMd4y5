package com.devSenior.sillon.pago.controlador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.pago.dto.ResultadoPago;
import com.devSenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devSenior.sillon.pago.dto.WebhookRespuesta;
import com.devSenior.sillon.pago.servicio.PagoServicio;
import com.devSenior.sillon.seguridad.UsuarioPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/pagos")
@Tag(name = "7. Pagos")
public class PagoControlador {

	private final PagoServicio pagoServicio;

	public PagoControlador(PagoServicio pagoServicio) {
		this.pagoServicio = pagoServicio;
	}

	@PostMapping("/suscripcion")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Crea la sesión de Checkout de la suscripción PRO. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Sesión de pago")
	@ApiResponse(responseCode = "409", description = "El salón ya es PRO")
	@ApiResponse(responseCode = "502", description = "Stripe no creó la sesión")
	public SesionPagoRespuesta suscripcion(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return pagoServicio.suscripcion(principal.getSalonId(), principal.getUsername());
	}

	@PostMapping("/creditos")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Crea la sesión de Checkout del paquete de créditos. Rol DUENO.")
	@ApiResponse(responseCode = "200", description = "Sesión de pago")
	@ApiResponse(responseCode = "502", description = "Stripe no creó la sesión")
	public SesionPagoRespuesta creditos(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return pagoServicio.creditos(principal.getSalonId(), principal.getUsername());
	}

	@PostMapping("/webhook")
	@SecurityRequirements
	@Operation(summary = "Recibe un evento de Stripe. Público, con firma.")
	@ApiResponse(responseCode = "200", description = "Evento recibido")
	@ApiResponse(responseCode = "400", description = "Firma inválida")
	public WebhookRespuesta webhook(HttpServletRequest peticion,
			@RequestHeader(name = "Stripe-Signature", required = false) String firma) {
		return pagoServicio.recibir(cuerpo(peticion), firma);
	}

	@GetMapping("/resultado")
	@SecurityRequirements
	@Operation(summary = "Página de resultado del pago. Pública.")
	@ApiResponse(responseCode = "200", description = "Resultado")
	@ApiResponse(responseCode = "400", description = "Estado inválido")
	public ResultadoPago resultado(@RequestParam String estado) {
		return pagoServicio.resultado(estado);
	}

	private static String cuerpo(HttpServletRequest peticion) {
		try {
			return new String(peticion.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new FirmaInvalidaException("La firma del webhook no es válida");
		}
	}

}

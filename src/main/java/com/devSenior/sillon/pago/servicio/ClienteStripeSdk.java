package com.devSenior.sillon.pago.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Component
public class ClienteStripeSdk implements ClienteStripe {

	private static final Logger log = LoggerFactory.getLogger(ClienteStripeSdk.class);

	private final StripeClient stripeClient;

	public ClienteStripeSdk(StripeClient stripeClient) {
		this.stripeClient = stripeClient;
	}

	@Override
	public SesionPagoRespuesta crear(TipoCompra tipo, String precioId, long salonId, String correo, String urlExito,
			String urlCancelacion) {
		SessionCreateParams.Mode modo = tipo == TipoCompra.SUSCRIPCION_PRO
				? SessionCreateParams.Mode.SUBSCRIPTION
				: SessionCreateParams.Mode.PAYMENT;
		SessionCreateParams parametros = SessionCreateParams.builder()
				.setMode(modo)
				.setSuccessUrl(urlExito)
				.setCancelUrl(urlCancelacion)
				.setCustomerEmail(correo)
				.setClientReferenceId(Long.toString(salonId))
				.putMetadata("salonId", Long.toString(salonId))
				.putMetadata("tipo", tipo.name())
				.addLineItem(SessionCreateParams.LineItem.builder()
						.setPrice(precioId)
						.setQuantity(1L)
						.build())
				.build();
		try {
			Session sesion = stripeClient.v1().checkout().sessions().create(parametros);
			if (sesion.getId() == null || sesion.getUrl() == null || sesion.getUrl().isBlank()) {
				throw new IntegracionInvalidaException("Stripe no devolvió la sesión de pago");
			}
			return new SesionPagoRespuesta(tipo, sesion.getId(), sesion.getUrl());
		} catch (StripeException e) {
			log.error("Stripe no creó la sesión. tipo={} estado={}", tipo, e.getStatusCode());
			throw new IntegracionInvalidaException("Stripe no pudo crear la sesión de pago");
		}
	}

}

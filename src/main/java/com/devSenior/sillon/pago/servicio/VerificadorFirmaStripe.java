package com.devSenior.sillon.pago.servicio;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.pago.config.StripePropiedades;
import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.stripe.StripeClient;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

@Component
public class VerificadorFirmaStripe {

	private static final Logger log = LoggerFactory.getLogger(VerificadorFirmaStripe.class);

	private final StripeClient stripeClient;
	private final StripePropiedades propiedades;

	public VerificadorFirmaStripe(StripeClient stripeClient, StripePropiedades propiedades) {
		this.stripeClient = stripeClient;
		this.propiedades = propiedades;
	}

	public EventoPago leer(String cuerpo, String firma) {
		if (cuerpo == null || cuerpo.isBlank() || firma == null || firma.isBlank()) {
			throw new FirmaInvalidaException("La firma del webhook no es válida");
		}
		Event evento;
		try {
			evento = stripeClient.constructEvent(cuerpo, firma, propiedades.getSecretoWebhook());
		} catch (SignatureVerificationException e) {
			throw new FirmaInvalidaException("La firma del webhook no es válida");
		}
		if (evento.getId() == null || evento.getId().isBlank() || evento.getType() == null) {
			throw new IntegracionInvalidaException("Stripe respondió un evento que no se pudo leer");
		}
		StripeObject objeto = objeto(evento);
		if ("checkout.session.completed".equals(evento.getType())) {
			if (!(objeto instanceof Session sesion)) {
				throw new IntegracionInvalidaException("Stripe respondió un checkout que no se pudo leer");
			}
			return new EventoPago(
					evento.getId(),
					evento.getType(),
					sesion.getPaymentStatus(),
					tipo(metadata(sesion, "tipo")),
					salon(sesion),
					sesion.getSubscription());
		}
		if ("customer.subscription.deleted".equals(evento.getType())) {
			if (!(objeto instanceof Subscription suscripcion) || suscripcion.getId() == null) {
				throw new IntegracionInvalidaException("Stripe respondió una suscripción que no se pudo leer");
			}
			return new EventoPago(evento.getId(), evento.getType(), null, null, null, suscripcion.getId());
		}
		return new EventoPago(evento.getId(), evento.getType(), null, null, null, null);
	}

	private StripeObject objeto(Event evento) {
		EventDataObjectDeserializer deserializador = evento.getDataObjectDeserializer();
		Optional<StripeObject> leido = Optional.empty();
		try {
			leido = deserializador.getObject();
		} catch (RuntimeException e) {
			leido = Optional.empty();
		}
		if (leido.isPresent()) {
			return leido.get();
		}
		log.error("Evento Stripe con objeto vacío; deserialización sin verificación de versión. evento={}",
				evento.getId());
		try {
			return deserializador.deserializeUnsafe();
		} catch (EventDataObjectDeserializationException | RuntimeException e) {
			return null;
		}
	}

	private static String metadata(Session sesion, String clave) {
		if (sesion.getMetadata() == null) {
			return null;
		}
		return sesion.getMetadata().get(clave);
	}

	private static Long salon(Session sesion) {
		Long salonId = numero(metadata(sesion, "salonId"));
		if (salonId != null) {
			return salonId;
		}
		return numero(sesion.getClientReferenceId());
	}

	private static Long numero(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(valor);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static TipoCompra tipo(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		try {
			return TipoCompra.valueOf(valor);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}

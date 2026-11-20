package com.devSenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.pago.config.StripePropiedades;
import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.stripe.StripeClient;
import com.stripe.net.Webhook;

class VerificadorFirmaStripeTest {

	private static final String SECRETO = "whsec_prueba";

	private VerificadorFirmaStripe verificador;

	@BeforeEach
	void preparar() {
		StripePropiedades propiedades = new StripePropiedades();
		propiedades.setClaveSecreta("sk_test_relleno");
		propiedades.setSecretoWebhook(SECRETO);
		propiedades.setUrlApi("https://api.stripe.com");
		StripeClient cliente = StripeClient.builder()
				.setApiKey(propiedades.getClaveSecreta())
				.setApiBase(propiedades.getUrlApi())
				.build();
		verificador = new VerificadorFirmaStripe(cliente, propiedades);
	}

	@Test
	void firmaInvalidaResponde400() {
		assertThrows(FirmaInvalidaException.class, () -> verificador.leer(checkout("evt_1"), "t=1,v1=falsa"));
		assertThrows(FirmaInvalidaException.class, () -> verificador.leer(checkout("evt_1"), " "));
	}

	@Test
	void firmaValidaLeeLaCompra() throws Exception {
		String cuerpo = checkout("evt_1");
		String firma = Webhook.Signature.generateSignatureHeader(cuerpo, SECRETO);

		EventoPago evento = verificador.leer(cuerpo, firma);

		assertEquals("evt_1", evento.id());
		assertEquals("checkout.session.completed", evento.tipo());
		assertEquals("paid", evento.estadoPago());
		assertEquals(TipoCompra.SUSCRIPCION_PRO, evento.tipoCompra());
		assertEquals(4L, evento.salonId());
		assertEquals("sub_1", evento.suscripcionId());
	}

	private static String checkout(String id) {
		return """
				{
				  "id": "%s",
				  "object": "event",
				  "type": "checkout.session.completed",
				  "data": {
				    "object": {
				      "id": "cs_1",
				      "object": "checkout.session",
				      "payment_status": "paid",
				      "subscription": "sub_1",
				      "client_reference_id": "4",
				      "metadata": {
				        "salonId": "4",
				        "tipo": "SUSCRIPCION_PRO"
				      }
				    }
				  }
				}
				""".formatted(id);
	}

}

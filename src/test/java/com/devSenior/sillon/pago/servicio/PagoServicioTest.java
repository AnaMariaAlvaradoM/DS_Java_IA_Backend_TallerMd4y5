package com.devSenior.sillon.pago.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.pago.config.StripePropiedades;
import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devSenior.sillon.pago.dto.WebhookRespuesta;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class PagoServicioTest {

	@Mock
	private SalonRepositorio salonRepositorio;

	@Mock
	private ClienteStripe clienteStripe;

	@Mock
	private VerificadorFirmaStripe verificadorFirmaStripe;

	@Mock
	private AplicadorWebhook aplicadorWebhook;

	private PagoServicio pagoServicio;

	@BeforeEach
	void preparar() {
		StripePropiedades propiedades = new StripePropiedades();
		propiedades.setPrecioPro("price_pro");
		propiedades.setPrecioCreditos("price_creditos");
		pagoServicio = new PagoServicio(salonRepositorio, clienteStripe, verificadorFirmaStripe, aplicadorWebhook,
				propiedades, "http://localhost:8080");
	}

	@Test
	void suscripcionDeSalonProNoLlamaAStripe() {
		Salon salon = new Salon();
		salon.setPlan(Plan.PRO);
		when(salonRepositorio.findById(4L)).thenReturn(Optional.of(salon));

		assertThrows(ConflictoException.class, () -> pagoServicio.suscripcion(4L, "dueno@salon.test"));
		verify(clienteStripe, never()).crear(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void suscripcionUsaElPrecioDelBackend() {
		Salon salon = new Salon();
		salon.setPlan(Plan.GRATIS);
		when(salonRepositorio.findById(4L)).thenReturn(Optional.of(salon));
		when(clienteStripe.crear(TipoCompra.SUSCRIPCION_PRO, "price_pro", 4L, "dueno@salon.test",
				"http://localhost:8080/api/pagos/resultado?estado=exito",
				"http://localhost:8080/api/pagos/resultado?estado=cancelado"))
				.thenReturn(new SesionPagoRespuesta(TipoCompra.SUSCRIPCION_PRO, "cs_1", "https://pago.test/cs_1"));

		SesionPagoRespuesta respuesta = pagoServicio.suscripcion(4L, "dueno@salon.test");

		assertEquals(TipoCompra.SUSCRIPCION_PRO, respuesta.tipo());
		assertEquals("cs_1", respuesta.sesionId());
		assertEquals("https://pago.test/cs_1", respuesta.urlPago());
	}

	@Test
	void webhookDelegaElEventoVerificado() {
		EventoPago evento = new EventoPago("evt_1", "checkout.session.completed", "paid",
				TipoCompra.PAQUETE_CREDITOS, 4L, null);
		when(verificadorFirmaStripe.leer("{}", "firma")).thenReturn(evento);
		when(aplicadorWebhook.aplicar(evento)).thenReturn(new WebhookRespuesta(true, true));

		WebhookRespuesta respuesta = pagoServicio.recibir("{}", "firma");

		assertEquals(true, respuesta.procesado());
	}

	@Test
	void resultadoRechazaUnEstadoDistinto() {
		assertEquals("Pago recibido. Stripe confirmará el plan y los créditos.",
				pagoServicio.resultado("exito").mensaje());
		assertThrows(FirmaInvalidaException.class, () -> pagoServicio.resultado("otro"));
	}

}

package com.devSenior.sillon.pago.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.devSenior.sillon.compartido.excepcion.ConflictoException;
import com.devSenior.sillon.compartido.excepcion.FirmaInvalidaException;
import com.devSenior.sillon.compartido.excepcion.IntegracionInvalidaException;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.pago.config.StripePropiedades;
import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.dto.ResultadoPago;
import com.devSenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devSenior.sillon.pago.dto.WebhookRespuesta;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class PagoServicio {

	private final SalonRepositorio salonRepositorio;
	private final ClienteStripe clienteStripe;
	private final VerificadorFirmaStripe verificadorFirmaStripe;
	private final AplicadorWebhook aplicadorWebhook;
	private final StripePropiedades propiedades;
	private final String urlPublica;

	public PagoServicio(SalonRepositorio salonRepositorio, ClienteStripe clienteStripe,
			VerificadorFirmaStripe verificadorFirmaStripe, AplicadorWebhook aplicadorWebhook,
			StripePropiedades propiedades, @Value("${sillon.url-publica}") String urlPublica) {
		this.salonRepositorio = salonRepositorio;
		this.clienteStripe = clienteStripe;
		this.verificadorFirmaStripe = verificadorFirmaStripe;
		this.aplicadorWebhook = aplicadorWebhook;
		this.propiedades = propiedades;
		this.urlPublica = urlPublica;
	}

	public SesionPagoRespuesta suscripcion(Long salonId, String correo) {
		Salon salon = salon(salonId);
		if (salon.getPlan() == Plan.PRO) {
			throw new ConflictoException("El salón ya tiene el plan PRO");
		}
		return crear(TipoCompra.SUSCRIPCION_PRO, propiedades.getPrecioPro(), salonId, correo);
	}

	public SesionPagoRespuesta creditos(Long salonId, String correo) {
		salon(salonId);
		return crear(TipoCompra.PAQUETE_CREDITOS, propiedades.getPrecioCreditos(), salonId, correo);
	}

	public WebhookRespuesta recibir(String cuerpo, String firma) {
		EventoPago evento = verificadorFirmaStripe.leer(cuerpo, firma);
		try {
			return aplicadorWebhook.aplicar(evento);
		} catch (DataIntegrityViolationException e) {
			return new WebhookRespuesta(true, false);
		}
	}

	public ResultadoPago resultado(String estado) {
		if ("exito".equals(estado)) {
			return new ResultadoPago("Pago recibido. Stripe confirmará el plan y los créditos.");
		}
		if ("cancelado".equals(estado)) {
			return new ResultadoPago("Pago cancelado. No se modificó el plan ni los créditos.");
		}
		throw new FirmaInvalidaException("estado: debe ser exito o cancelado");
	}

	private SesionPagoRespuesta crear(TipoCompra tipo, String precioId, Long salonId, String correo) {
		if (precioId == null || precioId.isBlank()) {
			throw new IntegracionInvalidaException("Stripe no tiene configurado el precio");
		}
		return clienteStripe.crear(tipo, precioId, salonId, correo, url("exito"), url("cancelado"));
	}

	private String url(String estado) {
		String base = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
		return base + "/api/pagos/resultado?estado=" + estado;
	}

	private Salon salon(Long salonId) {
		return salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
	}

}

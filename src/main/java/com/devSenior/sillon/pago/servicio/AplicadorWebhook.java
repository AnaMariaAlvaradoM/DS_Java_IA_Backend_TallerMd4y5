package com.devSenior.sillon.pago.servicio;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.pago.dto.EventoPago;
import com.devSenior.sillon.pago.dto.WebhookRespuesta;
import com.devSenior.sillon.pago.entidad.EventoStripe;
import com.devSenior.sillon.pago.entidad.TipoCompra;
import com.devSenior.sillon.pago.repositorio.EventoStripeRepositorio;
import com.devSenior.sillon.salon.entidad.Plan;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class AplicadorWebhook {

	private final EventoStripeRepositorio eventoStripeRepositorio;
	private final SalonRepositorio salonRepositorio;
	private final Clock reloj;
	private final int creditosPro;
	private final int creditosPaquete;

	public AplicadorWebhook(EventoStripeRepositorio eventoStripeRepositorio, SalonRepositorio salonRepositorio,
			Clock reloj, @Value("${sillon.creditos.pro}") int creditosPro,
			@Value("${sillon.creditos.paquete}") int creditosPaquete) {
		this.eventoStripeRepositorio = eventoStripeRepositorio;
		this.salonRepositorio = salonRepositorio;
		this.reloj = reloj;
		this.creditosPro = creditosPro;
		this.creditosPaquete = creditosPaquete;
	}

	@Transactional
	public WebhookRespuesta aplicar(EventoPago evento) {
		if (eventoStripeRepositorio.existsById(evento.id())) {
			return new WebhookRespuesta(true, false);
		}
		boolean procesado = false;
		if ("checkout.session.completed".equals(evento.tipo())) {
			procesado = aplicarCompra(evento);
		} else if ("customer.subscription.deleted".equals(evento.tipo())) {
			procesado = cancelar(evento);
		}
		EventoStripe registro = new EventoStripe();
		registro.setId(evento.id());
		registro.setTipo(evento.tipo());
		registro.setFechaProcesado(LocalDateTime.now(reloj));
		eventoStripeRepositorio.saveAndFlush(registro);
		return new WebhookRespuesta(true, procesado);
	}

	private boolean aplicarCompra(EventoPago evento) {
		if (!"paid".equals(evento.estadoPago()) || evento.salonId() == null || evento.tipoCompra() == null) {
			return false;
		}
		Salon salon = salonRepositorio.findById(evento.salonId()).orElse(null);
		if (salon == null) {
			return false;
		}
		if (evento.tipoCompra() == TipoCompra.SUSCRIPCION_PRO) {
			salon.setPlan(Plan.PRO);
			if (evento.suscripcionId() != null && !evento.suscripcionId().isBlank()) {
				salon.setStripeSuscripcionId(evento.suscripcionId());
			}
			salon.setCreditosIa(salon.getCreditosIa() + creditosPro);
		} else {
			salon.setCreditosIa(salon.getCreditosIa() + creditosPaquete);
		}
		salonRepositorio.save(salon);
		return true;
	}

	private boolean cancelar(EventoPago evento) {
		if (evento.suscripcionId() == null || evento.suscripcionId().isBlank()) {
			return false;
		}
		Salon salon = salonRepositorio.findByStripeSuscripcionId(evento.suscripcionId()).orElse(null);
		if (salon == null) {
			return false;
		}
		salon.setPlan(Plan.GRATIS);
		salon.setStripeSuscripcionId(null);
		salonRepositorio.save(salon);
		return true;
	}

}

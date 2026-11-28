package com.devSenior.sillon.ia.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class CreditoIaServicio {

	private final SalonRepositorio salonRepositorio;

	public CreditoIaServicio(SalonRepositorio salonRepositorio) {
		this.salonRepositorio = salonRepositorio;
	}

	@Transactional(readOnly = true)
	public void verificar(Long salonId) {
		Salon salon = salon(salonId);
		if (salon.getCreditosIa() <= 0) {
			throw new PagoRequeridoException("El salón no tiene créditos de IA");
		}
	}

	@Transactional
	public int consumir(Long salonId) {
		int actualizados = salonRepositorio.consumirCreditoSiHay(salonId);
		if (actualizados != 1) {
			throw new PagoRequeridoException("El salón no tiene créditos de IA");
		}
		return salon(salonId).getCreditosIa();
	}

	private Salon salon(Long salonId) {
		return salonRepositorio.findById(salonId)
				.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
	}

}

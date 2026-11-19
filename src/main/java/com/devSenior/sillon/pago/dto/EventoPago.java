package com.devSenior.sillon.pago.dto;

import com.devSenior.sillon.pago.entidad.TipoCompra;

public record EventoPago(
		String id,
		String tipo,
		String estadoPago,
		TipoCompra tipoCompra,
		Long salonId,
		String suscripcionId) {
}

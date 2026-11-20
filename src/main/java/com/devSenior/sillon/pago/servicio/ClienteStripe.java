package com.devSenior.sillon.pago.servicio;

import com.devSenior.sillon.pago.dto.SesionPagoRespuesta;
import com.devSenior.sillon.pago.entidad.TipoCompra;

public interface ClienteStripe {

	SesionPagoRespuesta crear(TipoCompra tipo, String precioId, long salonId, String correo, String urlExito,
			String urlCancelacion);

}

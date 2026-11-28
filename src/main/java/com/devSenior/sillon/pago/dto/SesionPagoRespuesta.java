package com.devSenior.sillon.pago.dto;

import com.devSenior.sillon.pago.entidad.TipoCompra;

public record SesionPagoRespuesta(TipoCompra tipo, String sesionId, String urlPago) {
}

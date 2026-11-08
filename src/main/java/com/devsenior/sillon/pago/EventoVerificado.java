package com.devsenior.sillon.pago;

public record EventoVerificado(String id, String tipo, Long salonId, TipoCompra tipoCompra,
        boolean pagado, String suscripcionId) {
}

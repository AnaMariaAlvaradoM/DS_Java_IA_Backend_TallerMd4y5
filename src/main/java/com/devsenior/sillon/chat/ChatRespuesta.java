package com.devsenior.sillon.chat;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record ChatRespuesta(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIA factura) {
}

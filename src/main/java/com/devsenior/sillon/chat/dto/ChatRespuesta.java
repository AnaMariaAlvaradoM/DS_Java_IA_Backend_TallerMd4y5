package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.ia.dto.FacturaIaDTO;

public record ChatRespuesta(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIaDTO factura
) {
}

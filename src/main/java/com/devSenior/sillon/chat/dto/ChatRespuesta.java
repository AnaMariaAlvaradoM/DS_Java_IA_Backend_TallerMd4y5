package com.devSenior.sillon.chat.dto;

import com.devSenior.sillon.ia.dto.FacturaIa;

public record ChatRespuesta(String respuesta, boolean escalado, Long solicitudId, FacturaIa factura) {
}

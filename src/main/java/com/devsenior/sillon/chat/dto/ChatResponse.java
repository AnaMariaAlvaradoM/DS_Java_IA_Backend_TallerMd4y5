package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.ia.FacturaIa;

public record ChatResponse(String respuesta, boolean escalado, Long solicitudId, FacturaIa factura) {
}

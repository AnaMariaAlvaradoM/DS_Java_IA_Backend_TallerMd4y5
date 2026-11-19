package com.devsenior.sillon.chat.dto;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record ChatResponse(String respuesta, boolean escalado, Long solicitudId, FacturaIA factura) {
}

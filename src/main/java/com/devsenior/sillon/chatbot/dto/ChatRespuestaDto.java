package com.devsenior.sillon.chatbot.dto;

import com.devsenior.sillon.ia.dto.FacturaIA;

public record ChatRespuestaDto(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        FacturaIA factura
) {}

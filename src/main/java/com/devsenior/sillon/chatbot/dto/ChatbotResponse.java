package com.devsenior.sillon.chatbot.dto;

import com.devsenior.sillon.ia.RespuestaIA;

public record ChatbotResponse(
        String respuesta,
        boolean escalado,
        Long solicitudId,
        RespuestaIA factura
) {
}

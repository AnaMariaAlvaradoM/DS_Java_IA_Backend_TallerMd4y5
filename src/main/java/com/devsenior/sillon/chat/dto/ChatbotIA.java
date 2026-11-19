package com.devsenior.sillon.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contrato con la IA: un JSON con las claves exactas "respuesta" (texto) y "escalar" (booleano). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatbotIA(String respuesta, Boolean escalar) {
}

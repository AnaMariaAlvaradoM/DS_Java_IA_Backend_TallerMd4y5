package com.devsenior.sillon.chat;

import com.devsenior.sillon.ia.Factura;

public record ChatRespuesta(String respuesta, boolean escalado, Long solicitudId, Factura factura) {
}

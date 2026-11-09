package com.devsenior.sillon.pago;

public interface VerificadorWebhook {
    EventoVerificado verificar(String cuerpo, String firma);
}

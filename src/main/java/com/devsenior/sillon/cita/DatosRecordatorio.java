package com.devsenior.sillon.cita;

/** Datos verificados que arma Java para el recordatorio. La IA solo redacta. */
public record DatosRecordatorio(Long citaId, String salonNombre, String clienteNombre, String clienteTelefono,
        String datos) {
}

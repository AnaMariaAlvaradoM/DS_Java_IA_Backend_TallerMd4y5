package com.devsenior.sillon.cita.dto;

/**
 * Datos verificados que arma Java para el recordatorio. La IA solo redacta con ellos.
 * datos: una línea por dato (Servicio, Estilista, Fecha, Hora, Valor, Dirección).
 */
public record DatosRecordatorio(
        Long citaId,
        Long salonId,
        String salonNombre,
        String clienteNombre,
        String clienteTelefono,
        String datos
) {
}

package com.devsenior.sillon.cita;

public record DatosRecordatorio(
        Long citaId,
        String nombreSalon,
        String nombreCliente,
        String telefonoCliente,
        String datos
) {
}

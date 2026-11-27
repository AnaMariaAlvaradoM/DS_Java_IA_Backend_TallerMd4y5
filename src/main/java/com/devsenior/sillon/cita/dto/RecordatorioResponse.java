package com.devsenior.sillon.cita.dto;













































































































































import com.devsenior.sillon.ia.RespuestaIA;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RecordatorioResponse(
        Long citaId,
        String telefono,
        String mensaje,
        int creditosRestantes,
        RespuestaIA factura // Mantén el nombre "factura" en la llave si la guía lo pide, aunque el tipo sea RespuestaIA
) {
    @JsonProperty("telefonoCliente")
    public String telefonoCliente() {
        return telefono;
    }

    @JsonProperty("proveedor")
    public String proveedor() {
        return factura == null ? null : factura.proveedor();
    }
}

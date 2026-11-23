
package com.devsenior.sillon.compartido;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "RespuestaError",
    description = "Formato estándar de los errores de la API Sillón"
)
public record RespuestaError(

    @Schema(description = "Código HTTP del error", example = "409")
    int codigo,

    @Schema(description = "Nombre estándar del error", example = "Conflict")
    String error,

    @Schema(description = "Explicación del error en español",
            example = "La cita se cruza con otra reserva")
    String mensaje,

    @Schema(description = "Fecha y hora en que ocurrió el error")
    LocalDateTime fecha

) {
}


package com.devsenior.sillon.ia.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudChatIa(

        @NotBlank @Size(max = 100) String clienteNombre,

        @NotBlank @Size(max = 120) String clienteContacto,

        @NotEmpty @Size(min = 1, max = 20) List<@NotNull @Valid MensajeHistorial> historial

) {

    public record MensajeHistorial(

            @NotNull RolMensaje rol,

            @NotBlank @Size(max = 500) String texto

    ) {
    }

    public enum RolMensaje {
        CLIENTE,
        ASISTENTE
    }
}

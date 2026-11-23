
package com.devsenior.sillon.cita;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CitaRequest(

        @NotBlank(message = "El nombre del cliente es obligatorio")
        String clienteNombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(min = 7, max = 30,
                message = "El teléfono debe tener entre 7 y 30 caracteres")
        String clienteTelefono,

        @NotNull(message = "El servicio es obligatorio")
        @Positive(message = "El servicio debe tener un ID válido")
        Long servicioId,

        @NotNull(message = "El estilista es obligatorio")
        @Positive(message = "El estilista debe tener un ID válido")
        Long estilistaId,

        @NotNull(message = "La fecha y hora son obligatorias")
        LocalDateTime inicio,

        @Size(max = 300,
                message = "La nota no puede superar 300 caracteres")
        String nota

) {
}

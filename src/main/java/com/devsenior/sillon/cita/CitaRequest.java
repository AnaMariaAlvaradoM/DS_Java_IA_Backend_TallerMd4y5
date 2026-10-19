package com.devsenior.sillon.cita;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record CitaRequest(
        @NotBlank(message = "es obligatorio") String clienteNombre,
        @NotBlank(message = "es obligatorio") @Size(min = 7, max = 30, message = "debe tener entre 7 y 30 caracteres")
        @Pattern(regexp = "(?s).*[0-9].*", message = "debe contener al menos un dígito") String clienteTelefono,
        @NotNull(message = "es obligatorio") Long servicioId,
        @NotNull(message = "es obligatorio") Long estilistaId,
        @NotNull(message = "es obligatorio") LocalDateTime inicio,
        @Size(max = 300, message = "máximo 300 caracteres") String nota) {
}

package com.devsenior.sillon.compartido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class Entradas {
    public record Registro(
            @NotBlank @Size(max = 100) String salonNombre,
            @NotBlank @Size(max = 60) String ciudad,
            @NotBlank @Size(max = 150) String direccion,
            @NotBlank @Size(max = 30) String telefono,
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}

    public record Configuracion(
            @NotNull LocalTime horaApertura,
            @NotNull LocalTime horaCierre,
            @NotNull Boolean abreDomingos,
            @NotNull @Min(15) @Max(60) Integer intervaloMinutos) {}

    public record NuevoServicio(
            @NotBlank @Size(max = 80) String nombre,
            @Size(max = 300) String descripcion,
            @NotNull @Min(15) @Max(480) Integer duracionMinutos,
            @NotNull @Positive @Digits(integer = 10, fraction = 0) BigDecimal precio,
            Boolean activo) {}

    public record Estilista(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> servicioIds) {}

    public record Reserva(
            @NotBlank @Size(max = 120) String clienteNombre,
            @NotBlank @Pattern(regexp = "[+0-9() -]{7,30}") String clienteTelefono,
            @NotNull @Positive Long servicioId,
            @NotNull @Positive Long estilistaId,
            @NotNull LocalDateTime inicio,
            @Size(max = 300) String nota) {}

    public record CambioEstado(
            @NotBlank @Pattern(regexp = "PENDIENTE|CONFIRMADA|COMPLETADA|CANCELADA|NO_ASISTIO")
                    String estado) {}

    public record Turno(
            @NotBlank @Pattern(regexp = "CLIENTE|ASISTENTE") String rol,
            @NotBlank @Size(max = 500) String texto) {}

    public record Chat(
            @NotBlank @Size(max = 120) String clienteNombre,
            @NotBlank @Size(max = 120) String clienteContacto,
            @NotEmpty @Size(max = 20) List<@NotNull @Valid Turno> historial) {}
}

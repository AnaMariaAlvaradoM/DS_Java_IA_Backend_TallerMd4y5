package co.sillon;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

final class Entradas {
    record Registro(@NotBlank @Size(max=120) String salonNombre, @NotBlank @Size(max=120) String ciudad,
                    @NotBlank @Size(max=200) String direccion, @NotBlank @Size(max=32) String telefono,
                    @NotBlank @Size(max=120) String nombre, @NotBlank @Email @Size(max=190) String email,
                    @NotBlank @Size(min=8,max=72) String password) {}
    record Login(@NotBlank @Email String email, @NotBlank @Size(max=72) String password) {}
    record Configuracion(@NotNull LocalTime horaApertura, @NotNull LocalTime horaCierre,
                         @NotNull Boolean abreDomingos, @NotNull @Min(1) @Max(240) Integer intervaloMinutos) {}
    record NuevoServicio(@NotBlank @Size(max=160) String nombre, @Size(max=1000) String descripcion,
                         @NotNull @Min(10) @Max(720) Integer duracionMinutos,
                         @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal precio) {}
    record Estilista(@NotBlank @Size(max=120) String nombre, @NotBlank @Email @Size(max=190) String email,
                     @NotBlank @Size(min=8,max=72) String password,
                     @NotEmpty @Size(max=100) List<@NotNull @Positive Long> servicioIds) {}
    record Reserva(@NotBlank @Size(max=120) String clienteNombre,
                   @NotBlank @Pattern(regexp="[+0-9() -]{7,30}") String clienteTelefono,
                   @NotNull @Positive Long servicioId, @NotNull @Positive Long estilistaId,
                   @NotNull LocalDateTime inicio, @Size(max=1000) String nota) {}
    record CambioEstado(@NotBlank @Pattern(regexp="PENDIENTE|CONFIRMADA|COMPLETADA|CANCELADA|NO_ASISTIO") String estado) {}
    record Turno(@NotBlank @Pattern(regexp="CLIENTE|ASISTENTE") String rol, @NotBlank @Size(max=2000) String texto) {}
    record Chat(@NotBlank @Size(max=120) String clienteNombre, @NotBlank @Size(max=190) String clienteContacto,
                @NotEmpty @Size(max=20) List<@NotNull @Valid Turno> historial) {}
}

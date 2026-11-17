package com.devSenior.sillon.usuario.dto;

import jakarta.validation.constraints.NotNull;

public record SolicitudActivo(@NotNull Boolean activo) {
}

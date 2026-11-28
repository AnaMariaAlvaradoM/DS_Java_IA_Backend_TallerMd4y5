package com.devSenior.sillon.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record SolicitudServiciosUsuario(@NotNull List<Long> servicioIds) {
}

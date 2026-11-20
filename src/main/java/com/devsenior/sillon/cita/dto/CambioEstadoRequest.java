package com.devsenior.sillon.cita.dto;

import com.devsenior.sillon.cita.EstadoCita;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

public record CambioEstadoRequest(@NotNull(message = "es obligatorio") EstadoCita estado) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static CambioEstadoRequest desdeJson(@JsonProperty("estado") JsonNode estado) {
        if (estado == null || estado.isNull()) {
            return new CambioEstadoRequest(null);
        }
        if (!estado.isString()) {
            throw new IllegalArgumentException("estado debe enviarse como texto");
        }
        return new CambioEstadoRequest(EstadoCita.valueOf(estado.stringValue()));
    }
}

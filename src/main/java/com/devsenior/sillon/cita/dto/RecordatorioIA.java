package com.devsenior.sillon.cita.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Contrato con la IA: un JSON con la clave exacta "mensaje". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordatorioIA(String mensaje) {
}

package com.devsenior.sillon.cita;

/** Copia inmutable de los datos comprobados antes de invocar IA. */
public record DatosRecordatorio(Long citaId, String salon, String cliente,
                               String telefonoCliente, String datos) { }

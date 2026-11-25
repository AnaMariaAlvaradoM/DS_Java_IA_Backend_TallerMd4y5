package com.devsenior.sillon.cita;

/**
 * Origen de creacion de la cita.
 * SALON: creada directamente por el personal del salon (nace CONFIRMADA).
 * EN_LINEA: reservada por un cliente final desde la web publica (nace PENDIENTE).
 */
public enum OrigenCita {
    SALON,
    EN_LINEA
}

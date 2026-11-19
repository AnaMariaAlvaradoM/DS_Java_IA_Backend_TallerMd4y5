package com.devsenior.sillon.cita.model;

import java.util.Set;

/**
 * Máquina de estados de la cita (SPEC 8.4).
 * PENDIENTE → CONFIRMADA o CANCELADA; CONFIRMADA → COMPLETADA, CANCELADA o NO_ASISTIO.
 * COMPLETADA, CANCELADA y NO_ASISTIO son finales.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    /** Estados que ocupan agenda: solo estos cuentan para cruces y disponibilidad. */
    public static final Set<EstadoCita> OCUPAN_AGENDA = Set.of(PENDIENTE, CONFIRMADA);

    public boolean puedeCambiarA(EstadoCita destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == COMPLETADA || destino == CANCELADA || destino == NO_ASISTIO;
            case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
        };
    }

    public boolean ocupaAgenda() {
        return OCUPAN_AGENDA.contains(this);
    }
}

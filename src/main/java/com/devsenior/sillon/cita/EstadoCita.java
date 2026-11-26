package com.devsenior.sillon.cita;

/**
 * Máquina de estados de la cita.
 * PENDIENTE → CONFIRMADA | CANCELADA
 * CONFIRMADA → COMPLETADA | CANCELADA | NO_ASISTIO
 * COMPLETADA, CANCELADA y NO_ASISTIO son finales.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public boolean puedeCambiarA(EstadoCita destino) {
        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == COMPLETADA || destino == CANCELADA || destino == NO_ASISTIO;
            case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
        };
    }

    /** Solo PENDIENTE y CONFIRMADA ocupan la agenda del estilista. */
    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }

    public static java.util.List<EstadoCita> queOcupanAgenda() {
        return java.util.List.of(PENDIENTE, CONFIRMADA);
    }
}

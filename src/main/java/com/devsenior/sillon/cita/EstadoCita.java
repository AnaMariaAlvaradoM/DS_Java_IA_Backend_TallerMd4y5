package com.devsenior.sillon.cita;

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
            default -> false;
        };
    }

    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}

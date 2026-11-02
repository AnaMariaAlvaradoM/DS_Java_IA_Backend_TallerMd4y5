package com.devsenior.sillon.cita;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public boolean puedePasarA(EstadoCita destino) {
        if (destino == null || this == destino) {
            return false;
        }

        return switch (this) {
            case PENDIENTE -> destino == CONFIRMADA || destino == CANCELADA;
            case CONFIRMADA -> destino == COMPLETADA || destino == CANCELADA || destino == NO_ASISTIO;
            case COMPLETADA, CANCELADA, NO_ASISTIO -> false; // Estados finales
        };
    }

    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}
package com.devsenior.sillon.cita.modelo;

import java.util.Set;

public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }

    public boolean puedeCambiarA(EstadoCita destino) {
        if (destino == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE -> Set.of(CONFIRMADA, CANCELADA).contains(destino);
            case CONFIRMADA -> Set.of(COMPLETADA, CANCELADA, NO_ASISTIO).contains(destino);
            case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
        };
    }
}
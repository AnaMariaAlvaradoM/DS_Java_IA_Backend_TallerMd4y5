package com.devsenior.sillon.cita;

import java.util.List;

/**
 * Estados de una cita y maquina de estados.
 * Transiciones permitidas:
 * - PENDIENTE -> CONFIRMADA o CANCELADA
 * - CONFIRMADA -> COMPLETADA, CANCELADA o NO_ASISTIO
 * COMPLETADA, CANCELADA y NO_ASISTIO son estados finales.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public boolean puedeTransicionarA(EstadoCita nuevo) {
        if (nuevo == null) {
            return false;
        }
        return switch (this) {
            case PENDIENTE -> nuevo == CONFIRMADA || nuevo == CANCELADA;
            case CONFIRMADA -> nuevo == COMPLETADA || nuevo == CANCELADA || nuevo == NO_ASISTIO;
            case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
        };
    }

    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }

    public static List<EstadoCita> estadosQueOcupanAgenda() {
        return List.of(PENDIENTE, CONFIRMADA);
    }
}

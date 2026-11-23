
package com.devsenior.sillon.cita;

public enum EstadoCita {

    PENDIENTE,
    CONFIRMADA,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public boolean puedeCambiarA(EstadoCita nuevoEstado) {

        if (nuevoEstado == null) {
            return false;
        }

        return switch (this) {
            case PENDIENTE ->
                    nuevoEstado == CONFIRMADA
                    || nuevoEstado == CANCELADA;

            case CONFIRMADA ->
                    nuevoEstado == COMPLETADA
                    || nuevoEstado == CANCELADA
                    || nuevoEstado == NO_ASISTIO;

            case COMPLETADA, CANCELADA, NO_ASISTIO -> false;
        };
    }

    public boolean ocupaAgenda() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}

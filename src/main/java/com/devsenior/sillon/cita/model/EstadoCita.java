package com.devsenior.sillon.cita.model;

public enum EstadoCita {
    PENDIENTE,
    CANCELADA,
   CONFIRMADA,
    COMPLETADA,
    NO_ASISTIO;
    public boolean puedeCambiarA(EstadoCita nuevoEstado) {
        switch (this) {
            case PENDIENTE:
                return nuevoEstado == CANCELADA || nuevoEstado == CONFIRMADA;
            case CONFIRMADA:
                return nuevoEstado == CANCELADA || nuevoEstado == COMPLETADA || nuevoEstado == NO_ASISTIO;
            case COMPLETADA:
                return false; // No se puede cambiar a otro estado desde COMPLETADA
            case CANCELADA:
                return false; // No se puede cambiar a otro estado desde CANCELADA
            case NO_ASISTIO:
                return false; // No se puede cambiar a otro estado desde NO_ASISTIO
            default:
                return false;
        }
    }

    public boolean ocupaTurno() {
        return this == PENDIENTE || this == CONFIRMADA;
    }
}

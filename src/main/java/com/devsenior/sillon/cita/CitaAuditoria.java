package com.devsenior.sillon.cita;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class CitaAuditoria {

    private final Clock clock;

    public CitaAuditoria(Clock clock) {
        this.clock = clock;
    }

    @PrePersist
    public void antesDeInsertar(Cita cita) {
        cita.registrarCreacion(ahora());
    }

    @PreUpdate
    public void antesDeActualizar(Cita cita) {
        cita.registrarActualizacion(ahora());
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}

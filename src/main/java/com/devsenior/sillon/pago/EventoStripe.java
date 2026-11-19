package com.devsenior.sillon.pago;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {

    @Id
    private String id;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fechaProcesado;

    protected EventoStripe() {
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public LocalDateTime getFechaProcesado() {
        return fechaProcesado;
    }
}

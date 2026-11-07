package com.devsenior.sillon.pago.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {
    @Id
    private String id; // El ID del evento de Stripe (ej. evt_12345)
    private String tipo;
    private LocalDateTime fechaProcesado;

    public EventoStripe() {
    }

    public EventoStripe(String id, String tipo) {
        this.id = id;
        this.tipo = tipo;
        this.fechaProcesado = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaProcesado() {
        return fechaProcesado;
    }

    public void setFechaProcesado(LocalDateTime fechaProcesado) {
        this.fechaProcesado = fechaProcesado;
    }
}

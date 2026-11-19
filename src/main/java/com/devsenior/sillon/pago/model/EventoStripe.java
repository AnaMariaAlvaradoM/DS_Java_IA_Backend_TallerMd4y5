package com.devsenior.sillon.pago.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Evento de Stripe ya procesado. Su id es el de Stripe: si llega repetido, se ignora (idempotencia). */
@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fechaProcesado;

    public EventoStripe() {
    }

    public EventoStripe(String id, String tipo, LocalDateTime fechaProcesado) {
        this.id = id;
        this.tipo = tipo;
        this.fechaProcesado = fechaProcesado;
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

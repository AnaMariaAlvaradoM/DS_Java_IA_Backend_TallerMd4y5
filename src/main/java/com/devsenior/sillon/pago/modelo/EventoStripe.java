package com.devsenior.sillon.pago.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {

    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fechaProcesamiento;

    public EventoStripe() {
    }

    public EventoStripe(String id, String tipo, LocalDateTime fechaProcesamiento) {
        this.id = id;
        this.tipo = tipo;
        this.fechaProcesamiento = fechaProcesamiento;
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

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }
}
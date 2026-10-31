package com.devsenior.sillon.pago;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {

    @Id
    private String id;

    private String tipo;

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

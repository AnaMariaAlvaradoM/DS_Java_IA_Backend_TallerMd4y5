
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
    @Column(nullable = false, length = 255)
    private String id;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private LocalDateTime fechaProcesado;

    public EventoStripe() {
    }

    public EventoStripe(
            String id,
            String tipo,
            LocalDateTime fechaProcesado
    ) {
        this.id = id;
        this.tipo = tipo;
        this.fechaProcesado = fechaProcesado;
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

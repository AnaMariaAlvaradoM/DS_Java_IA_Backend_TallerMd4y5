package com.devsenior.sillon.pago;

import jakarta.persistence.*;

@Entity
@Table(name = "eventos_stripe")
public class EventoPago {
    @Id private String id;
    private String tipo;
    private java.time.LocalDateTime fechaProcesado;

    @Column(unique = true, nullable = false)
    private String operacion;

    public String getId() {
        return id;
    }

    public void setId(String valor) {
        this.id = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String valor) {
        this.tipo = valor;
    }

    public java.time.LocalDateTime getFechaProcesado() {
        return fechaProcesado;
    }

    public void setFechaProcesado(java.time.LocalDateTime valor) {
        this.fechaProcesado = valor;
    }

    public String getOperacion() {
        return operacion;
    }

    public void setOperacion(String valor) {
        this.operacion = valor;
    }
}

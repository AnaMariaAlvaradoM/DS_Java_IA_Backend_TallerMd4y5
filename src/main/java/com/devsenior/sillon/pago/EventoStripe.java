package com.devsenior.sillon.pago;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Transient;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe implements Persistable<String> {
    @Id private String id;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false) private LocalDateTime fechaProcesado;
    @Transient private boolean nuevo = true;

    public EventoStripe() { }
    public EventoStripe(String id, String tipo, LocalDateTime fechaProcesado) {
        this.id = id; this.tipo = tipo; this.fechaProcesado = fechaProcesado;
    }
    @Override public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getFechaProcesado() { return fechaProcesado; }
    public void setFechaProcesado(LocalDateTime fechaProcesado) { this.fechaProcesado = fechaProcesado; }
    @Override @Transient public boolean isNew() { return nuevo; }
    @PostLoad @PostPersist
    private void marcarPersistido() { nuevo = false; }
}

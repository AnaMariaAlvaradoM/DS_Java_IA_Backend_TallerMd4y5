package com.devsenior.sillon.cita;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.compartido.Entidad;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.Usuario;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas", indexes = @Index(name = "idx_agenda", columnList = "estilista_id,inicio"))
public class Cita extends Entidad {
    @ManyToOne(optional = false)
    private Salon salon;

    @ManyToOne(optional = false)
    private Usuario estilista;

    @ManyToOne(optional = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    private Servicio servicio;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    private OrigenCita origen;

    private LocalDateTime inicio;
    private LocalDateTime fin;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    @Column(precision = 12, scale = 0)
    private BigDecimal precio;

    @Column(length = 1000)
    private String nota;

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon valor) {
        this.salon = valor;
    }

    public Usuario getEstilista() {
        return estilista;
    }

    public void setEstilista(Usuario valor) {
        this.estilista = valor;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente valor) {
        this.cliente = valor;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio valor) {
        this.servicio = valor;
    }

    public String getEstado() {
        return estado.name();
    }

    public void setEstado(String valor) {
        this.estado = EstadoCita.valueOf(valor);
    }

    public String getOrigen() {
        return origen.name();
    }

    public void setOrigen(String valor) {
        this.origen = OrigenCita.valueOf(valor);
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime valor) {
        this.inicio = valor;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime valor) {
        this.fin = valor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime valor) {
        this.fechaCreacion = valor;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime valor) {
        this.fechaActualizacion = valor;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal valor) {
        this.precio = valor;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String valor) {
        this.nota = valor;
    }
}

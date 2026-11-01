package com.devsenior.sillon.cita.model;

import com.devsenior.sillon.cliente.Cliente;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.servicio.Servicio;
import com.devsenior.sillon.usuario.model.Usuario;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
//id, inicio, fin, precio, estado, origen, nota,
//fechaCreacion, fechaActualizacion

//Muchos a uno con Salon, Cliente,
//Usuario (estilista) y Servicio
@Entity
@Table(name = "citas")
public class Cita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private int precio;

    @Enumerated(EnumType.STRING)
    private EstadoCita estado;

    @Enumerated(EnumType.STRING)
    private OrigenCita origen;
    private String nota;
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
    @Column(name = "fecha_fin")
    private LocalDateTime finalizacion;
    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY )
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToOne(fetch = FetchType.LAZY )
    @JoinColumn(name = "cliente_id",nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY )
    @JoinColumn(name = "estilista_id", nullable = false)
    private Usuario estilista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    public Cita() {
    }

    public Cita( int precio, EstadoCita estadoCita, OrigenCita origen, String nota, Salon salon, Cliente cliente, Usuario estilista, Servicio servicio) {
        this.precio = precio;
        this.estado = estadoCita;
        this.origen = origen;
        this.nota = nota;
        this.salon = salon;
        this.cliente = cliente;
        this.estilista = estilista;
        this.servicio = servicio;
    }

    public Cita(Salon salon, Cliente cliente, Usuario estilista, Servicio servicio, Integer precio, LocalDateTime inicio, OrigenCita origen) {
        this.salon = salon;
        this.cliente = cliente;
        this.estilista = estilista;
        this.servicio = servicio;
        this.precio = precio;
        this.fechaHora = inicio;
        this.origen=origen;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public EstadoCita getEstadoCita() {
        return estado;
    }

    public void setEstadoCita(EstadoCita estadoCita) {
        this.estado = estadoCita;
    }

    public OrigenCita getOrigen() {
        return origen;
    }

    public void setOrigen(OrigenCita origen) {
        this.origen = origen;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public Salon getSalon() {
        return salon;
    }
    public void setSalon(Salon salon) {
        this.salon = salon;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getEstilista() {
        return estilista;
    }

    public void setEstilista(Usuario estilista) {
        this.estilista = estilista;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public LocalDateTime getFinalizacion() {
        return finalizacion;
    }
}

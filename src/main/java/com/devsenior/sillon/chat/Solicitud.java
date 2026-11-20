package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.Entidad;
import com.devsenior.sillon.salon.Salon;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contacto")
public class Solicitud extends Entidad {
    @ManyToOne(optional = false)
    private Salon salon;

    private String clienteNombre;
    private String clienteContacto;

    @Column(length = 2000)
    private String mensajes;

    @Enumerated(EnumType.STRING)
    private MotivoSolicitud motivo;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    private LocalDateTime fechaCreacion;

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon valor) {
        this.salon = valor;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String valor) {
        this.clienteNombre = valor;
    }

    public String getClienteContacto() {
        return clienteContacto;
    }

    public void setClienteContacto(String valor) {
        this.clienteContacto = valor;
    }

    public String getMensajes() {
        return mensajes;
    }

    public void setMensajes(String valor) {
        this.mensajes = valor;
    }

    public String getMotivo() {
        return motivo.name();
    }

    public void setMotivo(String valor) {
        this.motivo = MotivoSolicitud.valueOf(valor);
    }

    public String getEstado() {
        return estado.name();
    }

    public void setEstado(String valor) {
        this.estado = EstadoSolicitud.valueOf(valor);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime valor) {
        this.fechaCreacion = valor;
    }
}

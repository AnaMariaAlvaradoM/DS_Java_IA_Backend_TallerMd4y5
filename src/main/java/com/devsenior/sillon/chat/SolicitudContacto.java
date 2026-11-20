package com.devsenior.sillon.chat;

import java.time.LocalDateTime;

import com.devsenior.sillon.salon.Salon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitudes_contacto")
public class SolicitudContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Lob
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String clienteNombre;

    @Column(nullable = false, length = 120)
    private String clienteContacto;

    @Column(nullable = false, length = 2000)
    private String mensajes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoSolicitud motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    protected SolicitudContacto() {
    }

    public SolicitudContacto(Salon salon, String clienteNombre, String clienteContacto, String mensajes,
            MotivoSolicitud motivo, LocalDateTime fechaCreacion) {
        this.salon = salon;
        this.clienteNombre = clienteNombre;
        this.clienteContacto = clienteContacto;
        this.mensajes = mensajes;
        this.motivo = motivo;
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() {
        return id;
    }

    public Salon getSalon() {
        return salon;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public String getClienteContacto() {
        return clienteContacto;
    }

    public String getMensajes() {
        return mensajes;
    }

    public MotivoSolicitud getMotivo() {
        return motivo;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}

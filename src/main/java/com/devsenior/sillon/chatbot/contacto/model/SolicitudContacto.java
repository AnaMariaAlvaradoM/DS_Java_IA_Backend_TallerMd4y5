package com.devsenior.sillon.chatbot.contacto.model;

import com.devsenior.sillon.salon.model.Salon;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contacto")
public class SolicitudContacto {
//id, clienteNombre, clienteContacto, mensajes,
//motivo, estado, fechaCreacion
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salon_id", nullable = false)
    private Long salonId;

    @Column(name = "cliente_nombre", nullable = false)
    private String clienteNombre;

    @Column(name = "cliente_contacto", nullable = false)
    private String clienteContacto;

    @Column(length = 2000, nullable = false)
    private String mensajes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoSolicitud motivo;

    @Column(name = "estado", nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne
    @JoinColumn(name = "salon_id", insertable = false, updatable = false)
    private Salon salon;


    public SolicitudContacto() {
    }

    public SolicitudContacto( Long salonId, String clienteNombre, String clienteContacto, String mensajes, MotivoSolicitud motivo, EstadoSolicitud estado, LocalDateTime fechaCreacion, Salon salon) {

        this.salonId = salonId;
        this.clienteNombre = clienteNombre;
        this.clienteContacto = clienteContacto;
        this.mensajes = mensajes;
        this.motivo = motivo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.salon = salon;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSalonId() {
        return salonId;
    }

    public void setSalonId(Long salonId) {
        this.salonId = salonId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteContacto() {
        return clienteContacto;
    }

    public void setClienteContacto(String clienteContacto) {
        this.clienteContacto = clienteContacto;
    }

    public String getMensajes() {
        return mensajes;
    }

    public void setMensajes(String mensajes) {
        this.mensajes = mensajes;
    }

    public MotivoSolicitud getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoSolicitud motivo) {
        this.motivo = motivo;
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

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon salon) {
        this.salon = salon;
    }
}
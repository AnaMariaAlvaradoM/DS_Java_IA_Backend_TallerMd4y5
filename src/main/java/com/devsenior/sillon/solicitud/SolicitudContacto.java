package com.devsenior.sillon.solicitud;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_contacto")
public class SolicitudContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "cliente_nombre", nullable = false)
    private String clienteNombre;

    @Column(name = "cliente_contacto", nullable = false)
    private String clienteContacto;

    @Column(nullable = false, length = 2000)
    private String mensajes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoSolicitud motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public SolicitudContacto() {}

    public SolicitudContacto(Salon salon, String clienteNombre, String clienteContacto, String mensajes, MotivoSolicitud motivo, LocalDateTime fechaCreacion) {
        this.salon = salon;
        this.clienteNombre = clienteNombre;
        this.clienteContacto = clienteContacto;
        this.mensajes = mensajes;
        this.motivo = motivo;
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() { return id; }
    public Salon getSalon() { return salon; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteContacto() { return clienteContacto; }
    public String getMensajes() { return mensajes; }
    public MotivoSolicitud getMotivo() { return motivo; }
    public EstadoSolicitud getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public void marcarComoAtendida() {
        this.estado = EstadoSolicitud.ATENDIDA;
    }
}

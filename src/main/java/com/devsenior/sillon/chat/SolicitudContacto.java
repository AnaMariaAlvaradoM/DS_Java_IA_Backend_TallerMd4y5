package com.devsenior.sillon.chat;

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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clienteNombre;
    @Column(length = 120)
    private String clienteContacto;
    @Column(length = 2000)
    private String mensajes;
    @Enumerated(EnumType.STRING)
    private MotivoSolicitud motivo;
    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;
    private LocalDateTime fechaCreacion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteContacto() { return clienteContacto; }
    public void setClienteContacto(String clienteContacto) { this.clienteContacto = clienteContacto; }
    public String getMensajes() { return mensajes; }
    public void setMensajes(String mensajes) { this.mensajes = mensajes; }
    public MotivoSolicitud getMotivo() { return motivo; }
    public void setMotivo(MotivoSolicitud motivo) { this.motivo = motivo; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Salon getSalon() { return salon; }
    public void setSalon(Salon salon) { this.salon = salon; }
}

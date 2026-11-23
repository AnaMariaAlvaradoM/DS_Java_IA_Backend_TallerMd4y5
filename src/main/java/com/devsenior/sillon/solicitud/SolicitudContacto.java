
package com.devsenior.sillon.solicitud;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.devsenior.sillon.salon.Salon;

import jakarta.persistence.*;

@Entity
@Table(name = "solicitudes_contacto")
public class SolicitudContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(nullable = false, length = 150)
    private String clienteNombre;

    @Column(nullable = false, length = 120)
    private String clienteContacto;

    @Column(nullable = false, length = 2000)
    private String mensajes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Motivo motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public enum Motivo {
        PEDIDO_POR_IA,
        REGLA_DE_SEGURIDAD
    }

    public enum Estado {
        PENDIENTE,
        ATENDIDA
    }

    public SolicitudContacto() {
    }

    @PrePersist
    public void antesDeCrear() {
        if (estado == null) {
            estado = Estado.PENDIENTE;
        }

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now(
                    ZoneId.of("America/Bogota")
            );
        }
    }

    public Long getId() {
        return id;
    }

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon salon) {
        this.salon = salon;
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

    public Motivo getMotivo() {
        return motivo;
    }

    public void setMotivo(Motivo motivo) {
        this.motivo = motivo;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}

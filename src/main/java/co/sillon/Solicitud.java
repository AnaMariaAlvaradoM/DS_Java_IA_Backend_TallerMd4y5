package co.sillon;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="solicitudes_contacto")
class Solicitud extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    String clienteNombre;
    String clienteContacto;
    @Column(length=2000) String mensajes;
    String motivo;
    String estado="PENDIENTE";
    LocalDateTime fechaCreacion;
}

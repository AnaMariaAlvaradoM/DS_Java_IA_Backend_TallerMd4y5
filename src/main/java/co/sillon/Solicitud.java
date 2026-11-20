package co.sillon;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
class Solicitud extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    String clienteNombre;
    String clienteContacto;
    @Column(length=2000) String mensaje;
    LocalDateTime fechaCreacion;
}

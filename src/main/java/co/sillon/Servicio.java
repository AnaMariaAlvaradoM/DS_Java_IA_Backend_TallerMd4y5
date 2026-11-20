package co.sillon;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
class Servicio extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    String nombre;
    @Column(length=1000) String descripcion;
    int duracionMinutos;
    @Column(precision=12, scale=2) BigDecimal precio;
}

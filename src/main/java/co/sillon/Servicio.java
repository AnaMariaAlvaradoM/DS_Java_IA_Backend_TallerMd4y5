package co.sillon;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="servicios")
class Servicio extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    boolean activo=true;
    String nombre;
    @Column(length=1000) String descripcion;
    int duracionMinutos;
    @Column(precision=12, scale=0) BigDecimal precio;
}

package co.sillon;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="citas", indexes=@Index(name="idx_agenda", columnList="estilista_id,inicio"))
class Cita extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    @ManyToOne(optional=false) Usuario estilista;
    @ManyToOne(optional=false) Cliente cliente;
    @ManyToOne(optional=false) Servicio servicio;
    String estado;
    String origen;
    LocalDateTime inicio;
    LocalDateTime fin;
    LocalDateTime fechaCreacion;
    LocalDateTime fechaActualizacion;
    @Column(precision=12, scale=0) BigDecimal precio;
    @Column(length=1000) String nota;
}

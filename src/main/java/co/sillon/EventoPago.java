package co.sillon;

import jakarta.persistence.*;

@Entity
@Table(name="eventos_stripe")
class EventoPago {
    @Id String id;
    String tipo;
    java.time.LocalDateTime fechaProcesado;
    @Column(unique=true, nullable=false) String operacion;
}

package co.sillon;

import jakarta.persistence.*;

@Entity
class EventoPago {
    @Id String id;
    @Column(unique=true, nullable=false) String operacion;
}

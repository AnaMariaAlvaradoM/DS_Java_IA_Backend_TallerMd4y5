package co.sillon;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
class Salon extends Entidad {
    @Column(nullable=false) String nombre;
    @Column(nullable=false, unique=true, length=160) String slug;
    String ciudad;
    String direccion;
    String telefono;
    String plan = "GRATIS";
    int creditosIa = 10;
    LocalTime horaApertura = LocalTime.of(8, 0);
    LocalTime horaCierre = LocalTime.of(18, 0);
    boolean abreDomingos;
    int intervaloMinutos = 30;
    @Column(unique=true) String suscripcion;
}

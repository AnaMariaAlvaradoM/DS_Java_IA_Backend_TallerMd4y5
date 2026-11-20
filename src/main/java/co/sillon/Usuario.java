package co.sillon;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
class Usuario extends Entidad {
    @ManyToOne(optional=false) Salon salon;
    String nombre;
    @Column(unique=true, nullable=false, length=190) String email;
    @Column(nullable=false) String passwordHash;
    @Column(nullable=false) String rol;
    @ManyToMany Set<Servicio> servicios = new LinkedHashSet<>();
}

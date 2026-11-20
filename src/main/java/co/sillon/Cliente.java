package co.sillon;

import jakarta.persistence.*;


@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"salon_id","telefono"}))
class Cliente extends Entidad {
    @ManyToOne(optional=false) @JoinColumn(name="salon_id") Salon salon;
    String nombre;
    @Column(nullable=false) String telefono;
}

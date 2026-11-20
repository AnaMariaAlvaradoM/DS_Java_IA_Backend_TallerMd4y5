package co.sillon;

import jakarta.persistence.*;


@Entity
@Table(name="clientes", uniqueConstraints=@UniqueConstraint(columnNames={"salon_id","telefono"}))
class Cliente extends Entidad {
    @ManyToOne(optional=false) @JoinColumn(name="salon_id") Salon salon;
    String nombre;
    @Column(nullable=false) String telefono;
}

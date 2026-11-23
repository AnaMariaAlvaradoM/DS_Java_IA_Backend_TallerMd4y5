
package com.devsenior.sillon.cliente;

import com.devsenior.sillon.salon.Salon;
import jakarta.persistence.*;

@Entity
@Table(
    name = "clientes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_cliente_salon_telefono",
        columnNames = {"salon_id", "telefono"}
    )
)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    @ManyToOne(optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;


    public Cliente() {
    }

    public Cliente(String nombre, String telefono, Salon salon) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.salon = salon;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon salon) {
        this.salon = salon;
    }

}

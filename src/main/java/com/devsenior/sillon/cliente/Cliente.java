package com.devsenior.sillon.cliente;

import com.devsenior.sillon.compartido.Entidad;
import com.devsenior.sillon.salon.Salon;

import jakarta.persistence.*;

@Entity
@Table(
        name = "clientes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"salon_id", "telefono"}))
public class Cliente extends Entidad {
    @ManyToOne(optional = false)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    private String nombre;

    @Column(nullable = false)
    private String telefono;

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon valor) {
        this.salon = valor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String valor) {
        this.nombre = valor;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String valor) {
        this.telefono = valor;
    }
}

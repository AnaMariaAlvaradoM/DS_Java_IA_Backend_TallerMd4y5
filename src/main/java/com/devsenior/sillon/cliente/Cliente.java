package com.devsenior.sillon.cliente;

import com.devsenior.sillon.salon.model.Salon;
import jakarta.persistence.*;
@Entity
@Table(
        name = "clientes",
        uniqueConstraints = {
                // Restricción única COMPUESTA: la combinación de salón + teléfono no se puede repetir
                @UniqueConstraint(name = "uk_cliente_salon_telefono", columnNames = {"salon_id", "telefono"})
        }
)
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    public Cliente() {
    }

    public Cliente(Long id, String nombre, Salon salon, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.salon = salon;
        this.telefono = telefono;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public static String normalizarTelefono(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[^0-9+]", "");
    }
}

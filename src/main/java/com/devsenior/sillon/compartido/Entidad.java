package com.devsenior.sillon.compartido;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class Entidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long valor) {
        this.id = valor;
    }
}

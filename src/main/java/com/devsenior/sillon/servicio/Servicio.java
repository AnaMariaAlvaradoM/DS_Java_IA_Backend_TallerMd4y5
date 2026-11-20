package com.devsenior.sillon.servicio;

import com.devsenior.sillon.compartido.Entidad;
import com.devsenior.sillon.salon.Salon;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
public class Servicio extends Entidad {
    @ManyToOne(optional = false)
    private Salon salon;

    private boolean activo = true;
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    private int duracionMinutos;

    @Column(precision = 12, scale = 0)
    private BigDecimal precio;

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon valor) {
        this.salon = valor;
    }

    public boolean getActivo() {
        return activo;
    }

    public void setActivo(boolean valor) {
        this.activo = valor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String valor) {
        this.nombre = valor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String valor) {
        this.descripcion = valor;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int valor) {
        this.duracionMinutos = valor;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal valor) {
        this.precio = valor;
    }
}

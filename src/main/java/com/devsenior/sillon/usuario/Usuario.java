package com.devsenior.sillon.usuario;

import com.devsenior.sillon.compartido.Entidad;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario extends Entidad {
    @ManyToOne(optional = false)
    private Salon salon;

    private String nombre;

    @Column(unique = true, nullable = false, length = 190)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Rol rol;

    private boolean activo = true;

    @ManyToMany
    @JoinTable(name = "estilista_servicios")
    private Set<Servicio> servicios = new LinkedHashSet<>();

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String valor) {
        this.email = valor;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String valor) {
        this.password = valor;
    }

    public String getRol() {
        return rol.name();
    }

    public void setRol(String valor) {
        this.rol = Rol.valueOf(valor);
    }

    public boolean getActivo() {
        return activo;
    }

    public void setActivo(boolean valor) {
        this.activo = valor;
    }

    public Set<Servicio> getServicios() {
        return servicios;
    }

    public void setServicios(Set<Servicio> valor) {
        this.servicios = valor;
    }
}

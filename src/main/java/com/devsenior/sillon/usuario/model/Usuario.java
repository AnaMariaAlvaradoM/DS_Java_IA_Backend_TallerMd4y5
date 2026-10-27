package com.devsenior.sillon.usuario.model;

import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.servicio.Servicio;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

import java.util.HashSet;

import java.util.Set;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Length(max=100, message = "nombre no puede tener mas de 100 caracteres")
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "estilista_servicios",
            joinColumns = @JoinColumn(name = "estilista_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )
    private Set<Servicio> servicios=new HashSet<>();

    public Usuario() {
    }

    public Set<Servicio> getServicios() {
        return servicios;
    }

    public void setServicios(Set<Servicio> servicios) {
        this.servicios = servicios;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon salon) {
        this.salon = salon;
    }

    public boolean ofreceServicio(Long servicioId) {
        if (servicioId == null || this.servicios == null) {
            return false;
        }
        return this.servicios.stream()
                .anyMatch(s -> servicioId.equals(s.getId()));
    }
}

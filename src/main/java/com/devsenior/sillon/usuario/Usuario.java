package com.devsenior.sillon.usuario;

import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.servicio.Servicio;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @ManyToMany
    @JoinTable(
            name = "estilista_servicios",
            joinColumns = @JoinColumn(name = "estilista_id"),
            inverseJoinColumns = @JoinColumn(name = "servicio_id")
    )

    private Set<Servicio> servicios = new HashSet<>();

    public Usuario(){

    }

    public Usuario(String nombre, String email, String password, Rol rol, Salon salon) {
        this.nombre = nombre;
        this.email = email;
        this.password = password;
        this.rol = rol;
        this.salon = salon;
        this.activo = true;
    }

    public boolean ofreceServicio(Long servicioId) {
        if (servicioId == null || servicios == null || servicios.isEmpty()) {
            return false;
        }
        return servicios.stream()
                .anyMatch(servicio -> servicio.getId() != null && servicio.getId().equals(servicioId));
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Set<Servicio> getServicios() {
        return servicios;
    }

    public void setServicios(Set<Servicio> servicios) {
        this.servicios = servicios;
    }
}

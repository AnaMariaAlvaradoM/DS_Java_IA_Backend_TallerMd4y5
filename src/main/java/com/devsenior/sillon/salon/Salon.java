package com.devsenior.sillon.salon;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "salones")
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(unique = true)
    private String slug;

    private String ciudad;

    private String direccion;

    private String telefono;

    @Enumerated(EnumType.STRING)
    private Plan plan;

    private int creditosIa;

    private String stripeSuscripcionId;

    @Embedded
    private ConfiguracionSalon configuracion;

    private LocalDateTime fechaCreacion;

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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public int getCreditosIa() {
        return creditosIa;
    }

    public void setCreditosIa(int creditosIa) {
        this.creditosIa = creditosIa;
    }

    public String getStripeSuscripcionId() {
        return stripeSuscripcionId;
    }

    public void setStripeSuscripcionId(String stripeSuscripcionId) {
        this.stripeSuscripcionId = stripeSuscripcionId;
    }

    public ConfiguracionSalon getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionSalon configuracion) {
        this.configuracion = configuracion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

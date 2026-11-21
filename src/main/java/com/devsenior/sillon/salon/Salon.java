package com.devsenior.sillon.salon;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "salones")
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 60)
    private String ciudad;

    @Column(nullable = false, length = 150)
    private String direccion;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan;

    @Column(name = "creditos_ia", nullable = false)
    private int creditosIa;

    @Column(name = "stripe_suscripcion_id", length = 100)
    private String stripeSuscripcionId;

    @Embedded
    private ConfiguracionSalon configuracion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Salon() {
    }

    public Salon(Long id, String nombre, String slug, String ciudad, String direccion, String telefono, Plan plan, int creditosIa, String stripeSuscripcionId, ConfiguracionSalon configuracion, LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.slug = slug;
        this.ciudad = ciudad;
        this.direccion = direccion;
        this.telefono = telefono;
        this.plan = plan;
        this.creditosIa = creditosIa;
        this.stripeSuscripcionId = stripeSuscripcionId;
        this.configuracion = configuracion;
        this.fechaCreacion = fechaCreacion;
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

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
import jakarta.persistence.UniqueConstraint;

/**
 * El salon es el inquilino (tenant) de la plataforma: todo dato de negocio pertenece a un salon.
 * Las demas entidades (usuarios, servicios, clientes, citas...) apuntan a este con muchos a uno;
 * por eso aqui no hay listas inversas.
 */
@Entity
@Table(name = "salones", uniqueConstraints = @UniqueConstraint(name = "uk_salones_slug", columnNames = "slug"))
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Identificador de la URL publica: barberia-la-navaja. Unico en toda la plataforma. */
    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 60)
    private String ciudad;

    @Column(length = 150)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan = Plan.GRATIS;

    @Column(name = "creditos_ia", nullable = false)
    private int creditosIa;

    /** Id de la suscripcion en Stripe. Es nulo mientras el salon no haya pagado el plan PRO. */
    @Column(name = "stripe_suscripcion_id", length = 100)
    private String stripeSuscripcionId;

    @Embedded
    private ConfiguracionSalon configuracion = ConfiguracionSalon.porDefecto();

    /** Se asigna desde el servicio con el Clock del proyecto, nunca con la hora del servidor. */
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public Long getId() {
        return id;
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

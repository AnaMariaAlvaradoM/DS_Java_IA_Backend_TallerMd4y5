package com.devsenior.sillon.salon;

import com.devsenior.sillon.compartido.Entidad;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "salones")
public class Salon extends Entidad {
    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    private String ciudad;
    private String direccion;
    private String telefono;

    @Enumerated(EnumType.STRING)
    private Plan plan = Plan.GRATIS;

    private int creditosIa = 10;
    @Embedded private ConfiguracionSalon configuracion = new ConfiguracionSalon();

    @Column(unique = true)
    private String stripeSuscripcionId;

    private java.time.LocalDateTime fechaCreacion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String valor) {
        this.nombre = valor;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String valor) {
        this.slug = valor;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String valor) {
        this.ciudad = valor;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String valor) {
        this.direccion = valor;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String valor) {
        this.telefono = valor;
    }

    public String getPlan() {
        return plan.name();
    }

    public void setPlan(String valor) {
        this.plan = Plan.valueOf(valor);
    }

    public int getCreditosIa() {
        return creditosIa;
    }

    public void setCreditosIa(int valor) {
        this.creditosIa = valor;
    }

    public LocalTime getHoraApertura() {
        return configuracion.getHoraApertura();
    }

    public void setHoraApertura(LocalTime valor) {
        configuracion.setHoraApertura(valor);
    }

    public LocalTime getHoraCierre() {
        return configuracion.getHoraCierre();
    }

    public void setHoraCierre(LocalTime valor) {
        configuracion.setHoraCierre(valor);
    }

    public boolean getAbreDomingos() {
        return configuracion.getAbreDomingos();
    }

    public void setAbreDomingos(boolean valor) {
        configuracion.setAbreDomingos(valor);
    }

    public int getIntervaloMinutos() {
        return configuracion.getIntervaloMinutos();
    }

    public void setIntervaloMinutos(int valor) {
        configuracion.setIntervaloMinutos(valor);
    }

    public String getStripeSuscripcionId() {
        return stripeSuscripcionId;
    }

    public void setStripeSuscripcionId(String valor) {
        this.stripeSuscripcionId = valor;
    }

    public java.time.LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(java.time.LocalDateTime valor) {
        this.fechaCreacion = valor;
    }
}

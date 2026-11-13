package com.devsenior.sillon.servicio;

import com.devsenior.sillon.salon.model.Salon;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

//id, nombre, descripcion, duracionMinutos, precio,
//activo
@Entity
@Table(name = "servicios")
public class Servicio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    @Length(max=100, message = "nombre no puede tener mas de 100 caracteres")
    private String nombre;
    @Column(nullable = false)
    @Length(max=200, message = "descripcion no puede tener mas de 200 caracteres")
    private String descripcion;
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;
    @Column(nullable = false)
    private Integer precio;
    @Column(nullable = false)
    private Boolean activo=true;
    @ManyToOne
    @JoinColumn(name = "id_salon", nullable = false)
    private Salon salon;

    public Servicio() {
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Salon getSalon() {
        return salon;
    }

    public void setSalon(Salon salon) {
        this.salon = salon;
    }
}

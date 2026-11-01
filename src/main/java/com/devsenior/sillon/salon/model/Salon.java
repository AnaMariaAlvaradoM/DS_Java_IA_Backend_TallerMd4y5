package com.devsenior.sillon.salon.model;

import com.devsenior.sillon.usuario.model.Usuario;
import jakarta.persistence.*;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

//id, nombre, slug (único), ciudad, direccion,
//telefono, plan, creditosIa, stripeSuscripcionId,
//configuracion, fechaCreacion

//salonNombre (máx. 100), ciudad (máx. 60),
//direccion (máx. 150), telefono (máx. 30),
//nombre (máx. 100), email (formato válido),
//password (8 a 72)

@Entity
@Table(name = "salon")
public class Salon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Length(max = 100, message = "El nombre del salón no puede tener más de 100 caracteres")
    private String nombresalon;
    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    @Length(max = 60, message = "El nombre de la ciudad no puede tener más de 60 caracteres")
    private String ciudad;

    @Column(nullable = false)
    @Length(max = 150, message = "La dirección no puede tener más de 150 caracteres")
    private String direccion;

    @Column(nullable = false)
    @Length(max = 30, message = "El teléfono no puede tener más de 30 caracteres")
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    private Integer creditosIa;
    @Column()
    private String stripeSubscriptionId;

    @Embedded
    private ConfiguracionSalon configuracion = new ConfiguracionSalon();

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;
    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Usuario> usuarios;
    public Salon() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public Plan getPlan() {
        return plan;
    }

    public String getNombresalon() {
        return nombresalon;
    }

    public void setNombresalon(String nombresalon) {
        this.nombresalon = nombresalon;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public Integer getCreditosIa() {
        return creditosIa;
    }

    public void setCreditosIa(Integer creditosIa) {
        this.creditosIa = creditosIa;
    }

    public ConfiguracionSalon getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionSalon configuracion) {
        this.configuracion = configuracion;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}

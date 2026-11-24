package com.devsenior.sillon.usuario.modelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.servicio.modelo.Servicio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 100)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rol rol;

	@Column(nullable = false)
	private boolean activo = true;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "salon_id", nullable = false)
	private Salon salon;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "estilista_servicios",
			joinColumns = @JoinColumn(name = "estilista_id"),
			inverseJoinColumns = @JoinColumn(name = "servicio_id"))
	private List<Servicio> servicios = new ArrayList<>();

	public Usuario() {
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
		if (email == null) {
			this.email = null;
			return;
		}
		this.email = email.trim().toLowerCase(Locale.ROOT);
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

	public List<Servicio> getServicios() {
		return servicios;
	}

	public void setServicios(List<Servicio> servicios) {
		this.servicios = servicios == null ? new ArrayList<>() : servicios;
	}

}

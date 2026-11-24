package com.devsenior.sillon.cita.modelo;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.servicio.modelo.Servicio;
import com.devsenior.sillon.usuario.modelo.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "citas")
@EntityListeners(AuditingEntityListener.class)
public class Cita {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "salon_id", nullable = false)
	private Salon salon;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "estilista_id", nullable = false)
	private Usuario estilista;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "servicio_id", nullable = false)
	private Servicio servicio;

	@Column(nullable = false)
	private LocalDateTime inicio;

	@Column(nullable = false)
	private LocalDateTime fin;

	@Column(nullable = false)
	private int precio;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoCita estado;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrigenCita origen;

	@Column(length = 300)
	private String nota;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime fechaCreacion;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime fechaActualizacion;

	public Cita() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Salon getSalon() {
		return salon;
	}

	public void setSalon(Salon salon) {
		this.salon = salon;
	}

	public Cliente getCliente() {
		return cliente;
	}

	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}

	public Usuario getEstilista() {
		return estilista;
	}

	public void setEstilista(Usuario estilista) {
		this.estilista = estilista;
	}

	public Servicio getServicio() {
		return servicio;
	}

	public void setServicio(Servicio servicio) {
		this.servicio = servicio;
	}

	public LocalDateTime getInicio() {
		return inicio;
	}

	public void setInicio(LocalDateTime inicio) {
		this.inicio = inicio;
	}

	public LocalDateTime getFin() {
		return fin;
	}

	public void setFin(LocalDateTime fin) {
		this.fin = fin;
	}

	public int getPrecio() {
		return precio;
	}

	public void setPrecio(int precio) {
		this.precio = precio;
	}

	public EstadoCita getEstado() {
		return estado;
	}

	public void setEstado(EstadoCita estado) {
		this.estado = estado;
	}

	public OrigenCita getOrigen() {
		return origen;
	}

	public void setOrigen(OrigenCita origen) {
		this.origen = origen;
	}

	public String getNota() {
		return nota;
	}

	public void setNota(String nota) {
		this.nota = nota;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}

	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

}

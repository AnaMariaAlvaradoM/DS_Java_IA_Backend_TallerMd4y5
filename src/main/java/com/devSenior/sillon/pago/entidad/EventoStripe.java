package com.devSenior.sillon.pago.entidad;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos_stripe")
public class EventoStripe {

	@Id
	@Column(nullable = false, length = 255)
	private String id;

	@Column(nullable = false, length = 80)
	private String tipo;

	@Column(nullable = false)
	private LocalDateTime fechaProcesado;

	public EventoStripe() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public LocalDateTime getFechaProcesado() {
		return fechaProcesado;
	}

	public void setFechaProcesado(LocalDateTime fechaProcesado) {
		this.fechaProcesado = fechaProcesado;
	}

}

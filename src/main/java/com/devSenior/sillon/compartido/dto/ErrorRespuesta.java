package com.devSenior.sillon.compartido.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ErrorRespuesta {

	private final int codigo;
	private final String error;
	private final String mensaje;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private final LocalDateTime fecha;

	public ErrorRespuesta(int codigo, String error, String mensaje, LocalDateTime fecha) {
		this.codigo = codigo;
		this.error = error;
		this.mensaje = mensaje;
		this.fecha = fecha;
	}

	public int getCodigo() {
		return codigo;
	}

	public String getError() {
		return error;
	}

	public String getMensaje() {
		return mensaje;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

}

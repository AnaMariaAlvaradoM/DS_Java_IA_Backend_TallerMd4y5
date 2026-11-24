package com.devsenior.sillon.cliente.mapper;

import org.springframework.stereotype.Component;

import com.devsenior.sillon.cliente.dto.RespuestaCliente;
import com.devsenior.sillon.cliente.modelo.Cliente;

@Component
public class ClienteMapper {

	public RespuestaCliente aRespuesta(Cliente cliente) {
		return new RespuestaCliente(
				cliente.getId(),
				cliente.getNombre(),
				cliente.getTelefono());
	}

}

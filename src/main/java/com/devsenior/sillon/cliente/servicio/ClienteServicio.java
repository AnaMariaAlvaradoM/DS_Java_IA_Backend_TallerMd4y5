package com.devsenior.sillon.cliente.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.sillon.cliente.dto.RespuestaCliente;
import com.devsenior.sillon.cliente.mapper.ClienteMapper;
import com.devsenior.sillon.cliente.repositorio.ClienteRepositorio;

@Service
public class ClienteServicio {

	private final ClienteRepositorio clienteRepositorio;
	private final ClienteMapper clienteMapper;

	public ClienteServicio(
			ClienteRepositorio clienteRepositorio,
			ClienteMapper clienteMapper) {

		this.clienteRepositorio = clienteRepositorio;
		this.clienteMapper = clienteMapper;
	}

	/**
	 * Lista los clientes registrados dentro del salón del usuario autenticado.
	 */
	@Transactional(readOnly = true)
	public List<RespuestaCliente> listar(Long salonId) {
		return clienteRepositorio.findAllBySalonIdOrderByIdAsc(salonId)
				.stream()
				.map(clienteMapper::aRespuesta)
				.toList();
	}

}

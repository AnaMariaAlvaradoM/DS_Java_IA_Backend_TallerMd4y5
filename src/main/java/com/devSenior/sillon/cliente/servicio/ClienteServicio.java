package com.devSenior.sillon.cliente.servicio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devSenior.sillon.cliente.dto.ClienteRespuesta;
import com.devSenior.sillon.cliente.entidad.Cliente;
import com.devSenior.sillon.cliente.repositorio.ClienteRepositorio;
import com.devSenior.sillon.compartido.excepcion.NoEncontradoException;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@Service
public class ClienteServicio {

	private final ClienteRepositorio clienteRepositorio;
	private final SalonRepositorio salonRepositorio;

	public ClienteServicio(ClienteRepositorio clienteRepositorio, SalonRepositorio salonRepositorio) {
		this.clienteRepositorio = clienteRepositorio;
		this.salonRepositorio = salonRepositorio;
	}

	@Transactional(readOnly = true)
	public List<ClienteRespuesta> listar(Long salonId) {
		return clienteRepositorio.findBySalon_IdOrderByIdAsc(salonId).stream()
				.map(cliente -> new ClienteRespuesta(cliente.getId(), cliente.getNombre(), cliente.getTelefono()))
				.toList();
	}

	@Transactional
	public Cliente buscarOCrear(Long salonId, String nombre, String telefono) {
		String normalizado = normalizarTelefono(telefono);
		return clienteRepositorio.findBySalon_IdAndTelefono(salonId, normalizado).orElseGet(() -> {
			Salon salon = salonRepositorio.findById(salonId)
					.orElseThrow(() -> new NoEncontradoException("El salón no existe"));
			Cliente cliente = new Cliente();
			cliente.setNombre(nombre == null ? "" : nombre.trim());
			cliente.setTelefono(normalizado);
			cliente.setSalon(salon);
			return clienteRepositorio.save(cliente);
		});
	}

	public static String normalizarTelefono(String telefono) {
		if (telefono == null) {
			return "";
		}
		StringBuilder normalizado = new StringBuilder();
		for (int i = 0; i < telefono.length(); i++) {
			char caracter = telefono.charAt(i);
			if (caracter == '+' || Character.isDigit(caracter)) {
				normalizado.append(caracter);
			}
		}
		return normalizado.toString();
	}

}

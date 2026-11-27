package com.devsenior.sillon.cliente.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cliente.dto.RespuestaCliente;
import com.devsenior.sillon.cliente.mapper.ClienteMapper;
import com.devsenior.sillon.cliente.modelo.Cliente;
import com.devsenior.sillon.cliente.repositorio.ClienteRepositorio;
import com.devsenior.sillon.salon.modelo.Salon;

class ClienteServicioTest {

	private ClienteRepositorio clienteRepositorio;
	private ClienteServicio clienteServicio;

	@BeforeEach
	void preparar() {
		clienteRepositorio = mock(ClienteRepositorio.class);
		clienteServicio = new ClienteServicio(clienteRepositorio, new ClienteMapper());
	}

	@Test
	void listarFiltraPorSalonYDevuelveCamposDelContrato() {
		Salon salon = new Salon();
		salon.setId(1L);

		Cliente cliente1 = new Cliente();
		cliente1.setId(20L);
		cliente1.setNombre("Laura Gómez");
		cliente1.setTelefono("+573001112233");
		cliente1.setSalon(salon);

		Cliente cliente2 = new Cliente();
		cliente2.setId(21L);
		cliente2.setNombre("Andrés Ruiz");
		cliente2.setTelefono("3114445566");
		cliente2.setSalon(salon);

		when(clienteRepositorio.findAllBySalonIdOrderByIdAsc(1L))
				.thenReturn(List.of(cliente1, cliente2));

		List<RespuestaCliente> resultado = clienteServicio.listar(1L);

		assertEquals(2, resultado.size());
		assertEquals(20L, resultado.get(0).id());
		assertEquals("Laura Gómez", resultado.get(0).nombre());
		assertEquals("+573001112233", resultado.get(0).telefono());
		assertEquals(21L, resultado.get(1).id());
		assertEquals("Andrés Ruiz", resultado.get(1).nombre());
		assertEquals("3114445566", resultado.get(1).telefono());

		verify(clienteRepositorio).findAllBySalonIdOrderByIdAsc(1L);
		verifyNoMoreInteractions(clienteRepositorio);
	}

}

package com.devSenior.sillon.cliente.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devSenior.sillon.cliente.entidad.Cliente;
import com.devSenior.sillon.cliente.repositorio.ClienteRepositorio;
import com.devSenior.sillon.salon.entidad.Salon;
import com.devSenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class ClienteServicioTest {

	@Mock
	private ClienteRepositorio clienteRepositorio;

	@Mock
	private SalonRepositorio salonRepositorio;

	@InjectMocks
	private ClienteServicio clienteServicio;

	@Test
	void normalizaYReutilizaElClienteDelMismoSalon() {
		assertEquals("+573001112233", ClienteServicio.normalizarTelefono(" +57 (300) 111-2233 "));
		Cliente existente = new Cliente();
		existente.setId(4L);
		existente.setTelefono("+573001112233");
		when(clienteRepositorio.findBySalon_IdAndTelefono(1L, "+573001112233")).thenReturn(Optional.of(existente));

		Cliente resultado = clienteServicio.buscarOCrear(1L, "Ana", " +57 (300) 111-2233 ");

		assertSame(existente, resultado);
		verify(clienteRepositorio, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void creaSiNoExisteYPermiteElMismoTelefonoEnOtroSalon() {
		when(clienteRepositorio.findBySalon_IdAndTelefono(1L, "3001112233")).thenReturn(Optional.empty());
		when(clienteRepositorio.findBySalon_IdAndTelefono(2L, "3001112233")).thenReturn(Optional.empty());
		Salon uno = new Salon();
		uno.setId(1L);
		Salon dos = new Salon();
		dos.setId(2L);
		when(salonRepositorio.findById(1L)).thenReturn(Optional.of(uno));
		when(salonRepositorio.findById(2L)).thenReturn(Optional.of(dos));
		when(clienteRepositorio.save(org.mockito.ArgumentMatchers.any(Cliente.class))).thenAnswer(invocacion -> {
			Cliente cliente = invocacion.getArgument(0);
			cliente.setId(cliente.getSalon().getId());
			return cliente;
		});

		Cliente primero = clienteServicio.buscarOCrear(1L, "Ana", "(300) 111 2233");
		Cliente segundo = clienteServicio.buscarOCrear(2L, "Ana", "3001112233");

		assertEquals("3001112233", primero.getTelefono());
		assertEquals("3001112233", segundo.getTelefono());
		assertEquals(1L, primero.getSalon().getId());
		assertEquals(2L, segundo.getSalon().getId());
	}

}

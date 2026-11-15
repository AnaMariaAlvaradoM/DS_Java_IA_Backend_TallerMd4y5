
package com.devsenior.sillon.cliente;

import com.devsenior.sillon.salon.Salon;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClienteServiceTest {

    @Test
    void reutilizarClienteExistenteSinDuplicarlo() {

        ClienteRepository repository = mock(ClienteRepository.class);
        Salon salon = mock(Salon.class);

        when(salon.getId()).thenReturn(1L);

        Cliente existente = new Cliente(
                "Juan Pérez",
                "3105551234",
                salon
        );

        when(repository.findBySalonIdAndTelefono(
                1L, "3105551234"
        )).thenReturn(Optional.of(existente));

        ClienteService service = new ClienteService(repository);

        Cliente resultado = service.buscarOCrear(
                "Juan Pérez",
                "310-555-1234",
                salon
        );

        assertSame(existente, resultado);

        verify(repository, never()).save(any(Cliente.class));
    }


    @Test
    void crearClienteCuandoNoExiste() {

        ClienteRepository repository = mock(ClienteRepository.class);
        Salon salon = mock(Salon.class);

        when(salon.getId()).thenReturn(1L);

        when(repository.findBySalonIdAndTelefono(
                1L, "3105551234"
        )).thenReturn(Optional.empty());

        when(repository.save(any(Cliente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ClienteService service = new ClienteService(repository);

        Cliente resultado = service.buscarOCrear(
                "Laura Gómez",
                "310 555 1234",
                salon
        );

        assertEquals("Laura Gómez", resultado.getNombre());
        assertEquals("3105551234", resultado.getTelefono());
        assertSame(salon, resultado.getSalon());

        verify(repository, times(1)).save(any(Cliente.class));
    }

}

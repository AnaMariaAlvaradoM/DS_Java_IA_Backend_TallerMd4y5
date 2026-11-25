package com.devsenior.sillon.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServicioTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private ClienteServicio clienteServicio;

    private Salon salon;

    @BeforeEach
    void setUp() {
        salon = new Salon();
    }

    @Test
    @DisplayName("buscarOCrear retorna cliente existente sin crear otro")
    void buscarOCrear_clienteExiste_retornaExistente() {
        Long salonId = 1L;
        String telefono = "(300) 111-2233";
        String telefonoNormalizado = "3001112233";

        Cliente existente = new Cliente(10L, "Valentina Ruiz", telefonoNormalizado, salon);
        when(clienteRepository.findBySalonIdAndTelefono(salonId, telefonoNormalizado))
                .thenReturn(Optional.of(existente));

        Cliente resultado = clienteServicio.buscarOCrear(salonId, "Valentina", telefono);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Valentina Ruiz", resultado.getNombre());
        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("buscarOCrear normaliza teléfono y crea cliente si no existe")
    void buscarOCrear_clienteNoExiste_creaConTelefonoNormalizado() {
        Long salonId = 1L;
        String telefono = "+57 (310) 222-3344";
        String telefonoNormalizado = "+573102223344";

        when(clienteRepository.findBySalonIdAndTelefono(salonId, telefonoNormalizado))
                .thenReturn(Optional.empty());
        when(salonRepository.findById(salonId)).thenReturn(Optional.of(salon));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(11L);
            return c;
        });

        Cliente resultado = clienteServicio.buscarOCrear(salonId, "Andrés Gómez", telefono);

        assertNotNull(resultado);
        assertEquals(11L, resultado.getId());
        assertEquals("Andrés Gómez", resultado.getNombre());
        assertEquals(telefonoNormalizado, resultado.getTelefono());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("listar retorna clientes del salón")
    void listar_retornaClientesDelSalon() {
        Long salonId = 1L;
        Cliente c1 = new Cliente(1L, "Valentina Ruiz", "3001112233", salon);
        Cliente c2 = new Cliente(2L, "Andrés Gómez", "3102223344", salon);

        when(clienteRepository.findBySalonId(salonId)).thenReturn(List.of(c1, c2));

        List<ClienteRespuesta> lista = clienteServicio.listar(salonId);

        assertEquals(2, lista.size());
        assertEquals("Valentina Ruiz", lista.get(0).nombre());
        assertEquals("Andrés Gómez", lista.get(1).nombre());
    }
}

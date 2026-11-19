package com.devsenior.sillon.cliente.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.cliente.mapper.ClienteMapper;
import com.devsenior.sillon.cliente.model.Cliente;
import com.devsenior.sillon.cliente.repository.ClienteRepository;
import com.devsenior.sillon.salon.model.Salon;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    private ClienteService clienteService;
    private Salon salon;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository, new ClienteMapper());
        salon = new Salon();
        salon.setId(1L);
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "'300 111 2233', 3001112233",
            "'+57 (310) 222-3344', +573102223344",
            "'604.555.1234', 6045551234"
    })
    void normalizarTelefono_dejaSoloDigitosYMas(String telefono, String esperado) {
        assertEquals(esperado, ClienteService.normalizarTelefono(telefono));
    }

    @Test
    void buscarOCrear_clienteExistente_noCreaOtro() {
        Cliente existente = new Cliente();
        when(clienteRepository.findBySalonIdAndTelefono(1L, "3001112233")).thenReturn(Optional.of(existente));

        Cliente resultado = clienteService.buscarOCrear(salon, "Valentina Ruiz", "300 111 2233");

        assertSame(existente, resultado);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void buscarOCrear_clienteNuevo_loGuardaConTelefonoNormalizado() {
        when(clienteRepository.findBySalonIdAndTelefono(1L, "3001112233")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.buscarOCrear(salon, " Valentina Ruiz ", "300 111 2233");

        assertEquals("3001112233", resultado.getTelefono());
        assertEquals("Valentina Ruiz", resultado.getNombre());
        assertSame(salon, resultado.getSalon());
    }
}

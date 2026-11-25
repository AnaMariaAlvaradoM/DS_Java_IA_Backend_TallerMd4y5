package com.devsenior.sillon.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.compartido.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;

class SolicitudServicioTest {

    private SolicitudContactoRepository repository;
    private SolicitudServicio servicio;
    private Salon salon;

    @BeforeEach
    void setUp() {
        repository = mock(SolicitudContactoRepository.class);
        servicio = new SolicitudServicio(repository);

        salon = new Salon();
        salon.setId(1L);
    }

    @Test
    @DisplayName("Listar solicitudes del salón devuelve las solicitudes más recientes primero")
    void listarSolicitudes_devuelveOrdenadas() {
        SolicitudContacto s1 = new SolicitudContacto();
        s1.setId(10L);
        s1.setSalon(salon);
        s1.setClienteNombre("Carlos");
        s1.setClienteContacto("300 123 4567");
        s1.setMensajes("Quiero agendar");
        s1.setMotivo(MotivoSolicitud.PEDIDO_POR_IA);
        s1.setEstado(EstadoSolicitud.PENDIENTE);
        s1.setFechaCreacion(LocalDateTime.now());

        when(repository.findBySalonIdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(s1));

        List<SolicitudContactoRespuesta> resultado = servicio.listarSolicitudes(1L);

        assertEquals(1, resultado.size());
        assertEquals("Carlos", resultado.get(0).clienteNombre());
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.get(0).estado());
    }

    @Test
    @DisplayName("Marcar solicitud atendida cambia su estado de PENDIENTE a ATENDIDA")
    void marcarAtendida_exito() {
        SolicitudContacto s = new SolicitudContacto();
        s.setId(20L);
        s.setSalon(salon);
        s.setClienteNombre("Ana");
        s.setClienteContacto("310 987 6543");
        s.setMensajes("Consulta");
        s.setMotivo(MotivoSolicitud.REGLA_DE_SEGURIDAD);
        s.setEstado(EstadoSolicitud.PENDIENTE);
        s.setFechaCreacion(LocalDateTime.now());

        when(repository.findByIdAndSalonId(20L, 1L)).thenReturn(Optional.of(s));
        when(repository.save(s)).thenReturn(s);

        SolicitudContactoRespuesta respuesta = servicio.marcarAtendida(20L, 1L);

        assertNotNull(respuesta);
        assertEquals(EstadoSolicitud.ATENDIDA, respuesta.estado());
        verify(repository).save(s);
    }

    @Test
    @DisplayName("Marcar solicitud que ya estaba ATENDIDA lanza ReglaNegocioException (409)")
    void marcarAtendida_yaAtendida_lanza409() {
        SolicitudContacto s = new SolicitudContacto();
        s.setId(20L);
        s.setSalon(salon);
        s.setEstado(EstadoSolicitud.ATENDIDA);

        when(repository.findByIdAndSalonId(20L, 1L)).thenReturn(Optional.of(s));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.marcarAtendida(20L, 1L));
    }

    @Test
    @DisplayName("Marcar solicitud inexistente o de otro salón lanza RecursoNoEncontradoException (404)")
    void marcarAtendida_inexistente_lanza404() {
        when(repository.findByIdAndSalonId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.marcarAtendida(99L, 1L));
    }
}

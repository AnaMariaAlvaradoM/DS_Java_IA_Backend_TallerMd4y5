package com.devsenior.sillon.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SolicitudServiceTest {
    private final SolicitudRepository repositorio = mock(SolicitudRepository.class);
    private final SalonRepository salones = mock(SalonRepository.class);
    private final SolicitudService servicio = new SolicitudService(repositorio, salones, new SolicitudMapper());

    @Test void crearLimitaMensajesYConservaCampos() {
        Salon salon = new Salon(); salon.setId(1L);
        when(salones.findById(1L)).thenReturn(Optional.of(salon));
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacion -> {
            SolicitudContacto solicitud = invocacion.getArgument(0); solicitud.setId(2L); return solicitud;
        });
        var resultado = servicio.crear(1L, "Ana", "3102223344", "a".repeat(2500), MotivoSolicitud.PEDIDO_POR_IA);
        assertEquals(2000, resultado.mensajes().length()); assertEquals("Ana", resultado.clienteNombre());
        assertEquals(EstadoSolicitud.PENDIENTE, resultado.estado()); assertEquals(MotivoSolicitud.PEDIDO_POR_IA, resultado.motivo());
    }

    @Test void solicitudAjenaEs404YNoGuarda() {
        when(repositorio.findByIdAndSalonId(2L, 9L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.atender(9L, 2L));
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test void truncarNoDivideUnEmojiAlFinal() {
        when(salones.findById(1L)).thenReturn(Optional.of(new Salon()));
        when(repositorio.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        var resultado = servicio.crear(1L, "Ana", "3102223344", "a".repeat(1999) + "😀fin", MotivoSolicitud.PEDIDO_POR_IA);
        assertEquals("a".repeat(1999), resultado.mensajes());
    }

    @Test void atenderDosVecesEs409YNoGuarda() {
        SolicitudContacto solicitud = new SolicitudContacto(); solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        when(repositorio.findByIdAndSalonId(2L, 1L)).thenReturn(Optional.of(solicitud));
        assertThrows(ReglaNegocioException.class, () -> servicio.atender(1L, 2L));
        verify(repositorio, never()).saveAndFlush(any());
    }
}

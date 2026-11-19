package com.devsenior.sillon.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.chat.mapper.SolicitudMapper;
import com.devsenior.sillon.chat.model.EstadoSolicitud;
import com.devsenior.sillon.chat.model.MotivoSolicitud;
import com.devsenior.sillon.chat.model.SolicitudContacto;
import com.devsenior.sillon.chat.repository.SolicitudContactoRepository;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.compartido.excepcion.ReglaNegocioException;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudContactoRepository solicitudRepository;
    @Mock
    private SalonRepository salonRepository;

    private SolicitudService solicitudService;

    @BeforeEach
    void setUp() {
        solicitudService = new SolicitudService(solicitudRepository, salonRepository, new SolicitudMapper());
    }

    @Test
    void crear_mensajesMuyLargos_losRecortaA2000Caracteres() {
        when(salonRepository.getReferenceById(1L)).thenReturn(new Salon());
        when(solicitudRepository.save(any(SolicitudContacto.class))).thenAnswer(inv -> inv.getArgument(0));

        solicitudService.crear(1L, "Juliana", "315 444 5566", "a".repeat(2500), MotivoSolicitud.PEDIDO_POR_IA);

        ArgumentCaptor<SolicitudContacto> guardada = ArgumentCaptor.forClass(SolicitudContacto.class);
        verify(solicitudRepository).save(guardada.capture());
        assertEquals(2000, guardada.getValue().getMensajes().length());
        assertEquals(EstadoSolicitud.PENDIENTE, guardada.getValue().getEstado());
    }

    @Test
    void marcarAtendida_yaAtendida_lanza409() {
        SolicitudContacto solicitud = new SolicitudContacto();
        solicitud.setEstado(EstadoSolicitud.ATENDIDA);
        when(solicitudRepository.findByIdAndSalonId(5L, 1L)).thenReturn(Optional.of(solicitud));

        assertThrows(ReglaNegocioException.class, () -> solicitudService.marcarAtendida(1L, 5L));
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void marcarAtendida_deOtroSalon_lanza404() {
        when(solicitudRepository.findByIdAndSalonId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> solicitudService.marcarAtendida(2L, 5L));
    }
}

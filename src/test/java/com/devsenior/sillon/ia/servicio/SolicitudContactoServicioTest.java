
package com.devsenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.dto.RespuestaSolicitudContacto;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.modelo.SolicitudContacto;
import com.devsenior.sillon.ia.repositorio.SolicitudContactoRepositorio;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;

@ExtendWith(MockitoExtension.class)
class SolicitudContactoServicioTest {

    private static final Long SALON_ID = 10L;
    private static final Long SOLICITUD_ID = 25L;

    @Mock
    private SolicitudContactoRepositorio solicitudRepositorio;

    @Mock
    private SalonRepositorio salonRepositorio;

    private SolicitudContactoServicio servicio;

    private ObjectMapper objectMapper;

    @BeforeEach
    void preparar() {

        objectMapper = new ObjectMapper();

        servicio = new SolicitudContactoServicio(
                solicitudRepositorio,
                salonRepositorio,
                objectMapper);
    }

    @Test
    void creaSolicitudPendienteYConservaHistorial()
            throws Exception {

        Salon salon = new Salon();
        salon.setId(SALON_ID);

        List<SolicitudChatIa.MensajeHistorial> mensajes = List.of(
                new SolicitudChatIa.MensajeHistorial(
                        SolicitudChatIa.RolMensaje.CLIENTE,
                        "Quiero reservar una cita"));

        when(salonRepositorio.findById(SALON_ID))
                .thenReturn(Optional.of(salon));

        when(solicitudRepositorio.saveAndFlush(
                any(SolicitudContacto.class)))
                .thenAnswer(invocacion -> {

                    SolicitudContacto solicitud = invocacion.getArgument(0);

                    solicitud.setId(SOLICITUD_ID);

                    return solicitud;
                });

        Long resultado = servicio.crearSolicitud(
                SALON_ID,
                "Ana",
                "3001234567",
                mensajes,
                MotivoSolicitud.PEDIDO_POR_IA);

        assertEquals(SOLICITUD_ID, resultado);

        ArgumentCaptor<SolicitudContacto> captor = ArgumentCaptor.forClass(
                SolicitudContacto.class);

        verify(solicitudRepositorio)
                .saveAndFlush(captor.capture());

        SolicitudContacto guardada = captor.getValue();

        assertEquals(SALON_ID, guardada.getSalon().getId());
        assertEquals("Ana", guardada.getClienteNombre());
        assertEquals("3001234567",
                guardada.getClienteContacto());

        assertEquals(
                MotivoSolicitud.PEDIDO_POR_IA,
                guardada.getMotivo());

        assertEquals(
                EstadoSolicitud.PENDIENTE,
                guardada.getEstado());

        var historialGuardado = objectMapper.readTree(
                guardada.getMensajes());

        assertEquals(1, historialGuardado.size());

        assertEquals(
                "CLIENTE",
                historialGuardado.get(0)
                        .get("rol").asText());

        assertEquals(
                "Quiero reservar una cita",
                historialGuardado.get(0)
                        .get("texto").asText());
    }

    @Test
    void listarConvierteJsonAMensajesEstructurados() {

        SolicitudContacto solicitud = construirSolicitud();

        when(solicitudRepositorio
                .buscarPorSalonOrdenadas(SALON_ID))
                .thenReturn(List.of(solicitud));

        List<RespuestaSolicitudContacto> resultado = servicio.listar(SALON_ID);

        assertEquals(1, resultado.size());

        RespuestaSolicitudContacto respuesta = resultado.get(0);

        assertEquals(SOLICITUD_ID, respuesta.id());
        assertEquals("Ana", respuesta.clienteNombre());

        assertEquals(
                EstadoSolicitud.PENDIENTE,
                respuesta.estado());

        assertEquals(
                MotivoSolicitud.PEDIDO_POR_IA,
                respuesta.motivo());

        assertEquals(1, respuesta.mensajes().size());

        assertEquals(
                SolicitudChatIa.RolMensaje.CLIENTE,
                respuesta.mensajes().get(0).rol());

        assertEquals(
                "Quiero reservar una cita",
                respuesta.mensajes().get(0).texto());

        verify(solicitudRepositorio)
                .buscarPorSalonOrdenadas(SALON_ID);
    }

    @Test
    void marcaSolicitudPendienteComoAtendida() {

        SolicitudContacto solicitud = construirSolicitud();

        when(solicitudRepositorio.buscarPorIdYSalon(
                SOLICITUD_ID, SALON_ID))
                .thenReturn(Optional.of(solicitud));

        RespuestaSolicitudContacto respuesta = servicio.marcarAtendida(
                SALON_ID,
                SOLICITUD_ID);

        assertEquals(
                EstadoSolicitud.ATENDIDA,
                respuesta.estado());

        assertEquals(
                EstadoSolicitud.ATENDIDA,
                solicitud.getEstado());

        verify(solicitudRepositorio)
                .saveAndFlush(solicitud);
    }

    @Test
    void rechazaSolicitudInexistenteODeOtroSalon() {

        when(solicitudRepositorio.buscarPorIdYSalon(
                SOLICITUD_ID, SALON_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> servicio.marcarAtendida(
                        SALON_ID, SOLICITUD_ID));

        verify(solicitudRepositorio, never())
                .saveAndFlush(any(SolicitudContacto.class));
    }

    @Test
    void rechazaSolicitudYaAtendida() {

        SolicitudContacto solicitud = construirSolicitud();

        solicitud.setEstado(
                EstadoSolicitud.ATENDIDA);

        when(solicitudRepositorio.buscarPorIdYSalon(
                SOLICITUD_ID, SALON_ID))
                .thenReturn(Optional.of(solicitud));

        assertThrows(
                ExcepcionConflicto.class,
                () -> servicio.marcarAtendida(
                        SALON_ID, SOLICITUD_ID));

        verify(solicitudRepositorio, never())
                .saveAndFlush(any(SolicitudContacto.class));
    }

    @Test
    void rechazaHistorialJsonInvalido() {

        SolicitudContacto solicitud = construirSolicitud();

        solicitud.setMensajes("{json-invalido");

        when(solicitudRepositorio
                .buscarPorSalonOrdenadas(SALON_ID))
                .thenReturn(List.of(solicitud));

        assertThrows(
                ExcepcionIntegracion.class,
                () -> servicio.listar(SALON_ID));
    }

    private SolicitudContacto construirSolicitud() {

        SolicitudContacto solicitud = new SolicitudContacto();

        solicitud.setId(SOLICITUD_ID);
        solicitud.setClienteNombre("Ana");
        solicitud.setClienteContacto("3001234567");

        solicitud.setMensajes(
                """
                        [
                          {
                            "rol": "CLIENTE",
                            "texto": "Quiero reservar una cita"
                          }
                        ]
                        """);

        solicitud.setMotivo(
                MotivoSolicitud.PEDIDO_POR_IA);

        solicitud.setEstado(
                EstadoSolicitud.PENDIENTE);

        solicitud.setFechaCreacion(
                LocalDateTime.of(
                        2026, 11, 25, 22, 50));

        return solicitud;
    }
}

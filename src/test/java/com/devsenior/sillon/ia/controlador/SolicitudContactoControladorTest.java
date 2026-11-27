
package com.devsenior.sillon.ia.controlador;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.dto.RespuestaSolicitudContacto;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.modelo.EstadoSolicitud;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.servicio.SolicitudContactoServicio;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitudContactoControladorTest {

    private static final Long SALON_ID = 10L;
    private static final Long SOLICITUD_ID = 25L;

    private static final String RUTA_LISTADO = "/api/solicitudes";

    private static final String RUTA_ATENDIDA = "/api/solicitudes/25/atendida";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitudContactoServicio solicitudContactoServicio;

    @Test
    void duenoPuedeListarSolicitudes() throws Exception {

        when(solicitudContactoServicio.listar(SALON_ID))
                .thenReturn(List.of(
                        construirRespuesta(EstadoSolicitud.PENDIENTE)));

        mockMvc.perform(
                get(RUTA_LISTADO)
                        .with(user(crearPrincipal(Rol.DUENO))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(25))
                .andExpect(jsonPath("$[0].clienteNombre")
                        .value("Ana"))
                .andExpect(jsonPath("$[0].clienteContacto")
                        .value("3001234567"))
                .andExpect(jsonPath("$[0].motivo")
                        .value("PEDIDO_POR_IA"))
                .andExpect(jsonPath("$[0].estado")
                        .value("PENDIENTE"))
                .andExpect(jsonPath("$[0].mensajes[0].rol")
                        .value("CLIENTE"))
                .andExpect(jsonPath("$[0].mensajes[0].texto")
                        .value("Quiero reservar una cita"))
                .andExpect(jsonPath("$[0].fechaCreacion").exists());

        verify(solicitudContactoServicio)
                .listar(SALON_ID);
    }

    @Test
    void listadoSinAutenticacionDevuelve401()
            throws Exception {

        mockMvc.perform(get(RUTA_LISTADO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void estilistaNoPuedeListarSolicitudes()
            throws Exception {

        mockMvc.perform(
                get(RUTA_LISTADO)
                        .with(user(crearPrincipal(Rol.ESTILISTA))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void duenoPuedeMarcarSolicitudAtendida()
            throws Exception {

        when(solicitudContactoServicio.marcarAtendida(
                SALON_ID, SOLICITUD_ID))
                .thenReturn(
                        construirRespuesta(EstadoSolicitud.ATENDIDA));

        mockMvc.perform(
                patch(RUTA_ATENDIDA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.estado")
                        .value("ATENDIDA"))
                .andExpect(jsonPath("$.clienteNombre")
                        .value("Ana"));

        verify(solicitudContactoServicio)
                .marcarAtendida(SALON_ID, SOLICITUD_ID);
    }

    @Test
    void marcarAtendidaSinAutenticacionDevuelve401()
            throws Exception {

        mockMvc.perform(
                patch(RUTA_ATENDIDA)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void estilistaNoPuedeMarcarSolicitudAtendida()
            throws Exception {

        mockMvc.perform(
                patch(RUTA_ATENDIDA)
                        .with(user(crearPrincipal(Rol.ESTILISTA)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void solicitudInexistenteDevuelve404()
            throws Exception {

        when(solicitudContactoServicio.marcarAtendida(
                SALON_ID, SOLICITUD_ID))
                .thenThrow(new ExcepcionRecursoNoEncontrado(
                        "Solicitud no encontrada"));

        mockMvc.perform(
                patch(RUTA_ATENDIDA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(solicitudContactoServicio)
                .marcarAtendida(SALON_ID, SOLICITUD_ID);
    }

    @Test
    void solicitudYaAtendidaDevuelve409()
            throws Exception {

        when(solicitudContactoServicio.marcarAtendida(
                SALON_ID, SOLICITUD_ID))
                .thenThrow(new ExcepcionConflicto(
                        "La solicitud ya fue atendida"));

        mockMvc.perform(
                patch(RUTA_ATENDIDA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isConflict());

        verify(solicitudContactoServicio)
                .marcarAtendida(SALON_ID, SOLICITUD_ID);
    }

    private RespuestaSolicitudContacto construirRespuesta(
            EstadoSolicitud estado) {

        return new RespuestaSolicitudContacto(
                SOLICITUD_ID,
                "Ana",
                "3001234567",
                List.of(
                        new SolicitudChatIa.MensajeHistorial(
                                SolicitudChatIa.RolMensaje.CLIENTE,
                                "Quiero reservar una cita")),
                MotivoSolicitud.PEDIDO_POR_IA,
                estado,
                LocalDateTime.of(2026, 11, 25, 22, 50));
    }

    private UsuarioPrincipal crearPrincipal(Rol rol) {

        Salon salon = new Salon();
        salon.setId(SALON_ID);

        Usuario usuario = new Usuario();
        usuario.setId(100L);
        usuario.setSalon(salon);
        usuario.setNombre("Usuario de prueba");
        usuario.setEmail("prueba@sillon.test");
        usuario.setPassword("password-de-prueba");
        usuario.setRol(rol);
        usuario.setActivo(true);

        return new UsuarioPrincipal(usuario);
    }
}


package com.devsenior.sillon.ia.controlador;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.devsenior.sillon.compartido.excepcion.ExcepcionConflicto;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaRecordatorioIa;
import com.devsenior.sillon.ia.servicio.RecordatorioIaServicio;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.seguridad.UsuarioPrincipal;
import com.devsenior.sillon.usuario.modelo.Rol;
import com.devsenior.sillon.usuario.modelo.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
class RecordatorioIaControladorTest {

    private static final Long SALON_ID = 10L;
    private static final Long CITA_ID = 25L;

    private static final String RUTA =
            "/api/citas/25/recordatorio";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordatorioIaServicio recordatorioIaServicio;

    @Test
    void duenoPuedeGenerarRecordatorio()
            throws Exception {

        FacturaIa factura = new FacturaIa(
                "Gemini",
                "gemini-2.5-flash",
                100,
                35,
                BigDecimal.ZERO,
                150);

        RespuestaRecordatorioIa respuesta =
                new RespuestaRecordatorioIa(
                        CITA_ID,
                        "3001234567",
                        "Hola Ana, recuerda tu cita.",
                        4,
                        factura);

        when(recordatorioIaServicio.generar(
                SALON_ID, CITA_ID))
                .thenReturn(respuesta);

        mockMvc.perform(
                post(RUTA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citaId")
                        .value(25))
                .andExpect(jsonPath("$.telefonoCliente")
                        .value("3001234567"))
                .andExpect(jsonPath("$.mensaje")
                        .value("Hola Ana, recuerda tu cita."))
                .andExpect(jsonPath("$.creditosRestantes")
                        .value(4))
                .andExpect(jsonPath("$.factura.proveedor")
                        .value("Gemini"))
                .andExpect(jsonPath("$.factura.modelo")
                        .value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.factura.tokensEntrada")
                        .value(100))
                .andExpect(jsonPath("$.factura.tokensSalida")
                        .value(35));

        verify(recordatorioIaServicio, times(1))
                .generar(SALON_ID, CITA_ID);
    }

    @Test
    void usuarioSinAutenticacionRecibe401()
            throws Exception {

        mockMvc.perform(
                post(RUTA)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(recordatorioIaServicio);
    }

    @Test
    void estilistaNoPuedeGenerarRecordatorios()
            throws Exception {

        mockMvc.perform(
                post(RUTA)
                        .with(user(crearPrincipal(Rol.ESTILISTA)))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recordatorioIaServicio);
    }

    @Test
    void saldoAgotadoDevuelve402()
            throws Exception {

        when(recordatorioIaServicio.generar(
                SALON_ID, CITA_ID))
                .thenThrow(
                        new ExcepcionPagoRequerido(
                                "Sin creditos de IA"));

        mockMvc.perform(
                post(RUTA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isPaymentRequired());

        verify(recordatorioIaServicio)
                .generar(SALON_ID, CITA_ID);
    }

    @Test
    void citaInexistenteDevuelve404()
            throws Exception {

        when(recordatorioIaServicio.generar(
                SALON_ID, CITA_ID))
                .thenThrow(
                        new ExcepcionRecursoNoEncontrado(
                                "Cita no encontrada"));

        mockMvc.perform(
                post(RUTA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(recordatorioIaServicio)
                .generar(SALON_ID, CITA_ID);
    }

    @Test
    void estadoNoPermitidoDevuelve409()
            throws Exception {

        when(recordatorioIaServicio.generar(
                SALON_ID, CITA_ID))
                .thenThrow(
                        new ExcepcionConflicto(
                                "Estado no permitido"));

        mockMvc.perform(
                post(RUTA)
                        .with(user(crearPrincipal(Rol.DUENO)))
                        .with(csrf()))
                .andExpect(status().isConflict());

        verify(recordatorioIaServicio)
                .generar(SALON_ID, CITA_ID);
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

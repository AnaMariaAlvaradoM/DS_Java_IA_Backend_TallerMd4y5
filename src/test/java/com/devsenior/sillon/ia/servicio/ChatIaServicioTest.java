
package com.devsenior.sillon.ia.servicio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.excepcion.ExcepcionIntegracion;
import com.devsenior.sillon.compartido.excepcion.ExcepcionPagoRequerido;
import com.devsenior.sillon.compartido.excepcion.ExcepcionRecursoNoEncontrado;
import com.devsenior.sillon.compartido.excepcion.ExcepcionServicioNoDisponible;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaChatIa;
import com.devsenior.sillon.ia.dto.ResultadoProveedorIa;
import com.devsenior.sillon.ia.dto.SolicitudChatIa;
import com.devsenior.sillon.ia.dto.SolicitudChatIa.MensajeHistorial;
import com.devsenior.sillon.ia.dto.SolicitudChatIa.RolMensaje;
import com.devsenior.sillon.ia.modelo.MotivoSolicitud;
import com.devsenior.sillon.ia.proveedor.AsistenteIa;
import com.devsenior.sillon.publico.dto.RespuestaCatalogoPublico;
import com.devsenior.sillon.publico.dto.RespuestaServicioPublico;
import com.devsenior.sillon.publico.servicio.PublicoServicio;
import com.devsenior.sillon.salon.modelo.Salon;
import com.devsenior.sillon.salon.repositorio.SalonRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatIaServicioTest {

    private static final String SLUG = "salon-prueba";
    private static final Long SALON_ID = 7L;

    @Mock
    private SalonRepositorio salonRepositorio;

    @Mock
    private PublicoServicio publicoServicio;

    @Mock
    private CreditoIaServicio creditoIaServicio;

    @Mock
    private AsistenteIa proveedorIa;

    @Mock
    private SolicitudContactoServicio solicitudContactoServicio;

    @Mock
    private Salon salon;

    private ValidadorJsonIa validadorJsonIa;
    private ChatIaServicio chatIaServicio;

    @BeforeEach
    void configurar() {

        validadorJsonIa = spy(
                new ValidadorJsonIa(new ObjectMapper()));

        chatIaServicio = new ChatIaServicio(
                salonRepositorio,
                publicoServicio,
                creditoIaServicio,
                proveedorIa,
                validadorJsonIa,
                solicitudContactoServicio);
    }

    @Test
    void respondeConContextoDelCatalogoReal() {

        prepararSalonYCatalogo();

        String contenido = """
                {"respuesta":"El corte cuesta 25000 COP.",
                 "escalar":false}
                """;

        prepararProveedor(contenido);

        RespuestaChatIa respuesta = chatIaServicio.responder(
                SLUG,
                solicitud("Cuanto cuesta el corte?"));

        assertEquals(
                "El corte cuesta 25000 COP.",
                respuesta.respuesta());

        assertFalse(respuesta.escalado());
        assertNull(respuesta.solicitudId());
        assertNotNull(respuesta.factura());

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);

        verify(proveedorIa).generar(
                instrucciones.capture(),
                anyString());

        assertTrue(
                instrucciones.getValue()
                        .contains("Corte de cabello"));

        assertTrue(
                instrucciones.getValue()
                        .contains("25000 COP"));

        InOrder orden = inOrder(
                creditoIaServicio,
                proveedorIa,
                validadorJsonIa);

        orden.verify(creditoIaServicio)
                .verificarSaldo(SALON_ID);

        orden.verify(proveedorIa)
                .generar(anyString(), anyString());

        orden.verify(creditoIaServicio)
                .consumirCredito(SALON_ID);

        orden.verify(validadorJsonIa)
                .interpretar(contenido);

        verify(creditoIaServicio, times(1))
                .consumirCredito(SALON_ID);

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void rechazaSolicitudSinCreditosAntesDeLlamarIa() {

        prepararSalonYCatalogo();

        doThrow(new ExcepcionPagoRequerido(
                "Sin creditos disponibles"))
                .when(creditoIaServicio)
                .verificarSaldo(SALON_ID);

        assertThrows(
                ExcepcionPagoRequerido.class,
                () -> chatIaServicio.responder(
                        SLUG,
                        solicitud("Cuanto cuesta el corte?")));

        verifyNoInteractions(proveedorIa);

        verify(creditoIaServicio, never())
                .consumirCredito(anyLong());

        verifyNoInteractions(solicitudContactoServicio);
        verifyNoInteractions(validadorJsonIa);
    }

    @Test
    void escalaSolicitudDeCancelacion() {

        prepararSalonYCatalogo();

        prepararProveedor(
                """
                        {"respuesta":"Te puedo ayudar con eso.",
                         "escalar":false}
                        """);

        when(solicitudContactoServicio.crearSolicitud(
                eq(SALON_ID),
                eq("Ana"),
                eq("3001234567"),
                anyList(),
                eq(MotivoSolicitud.PEDIDO_POR_IA)))
                .thenReturn(90L);

        RespuestaChatIa respuesta = chatIaServicio.responder(
                SLUG,
                solicitud("Quiero cancelar mi cita"));

        assertTrue(respuesta.escalado());
        assertEquals(90L, respuesta.solicitudId());

        assertTrue(
                respuesta.respuesta()
                        .contains("equipo del salon"));

        verify(solicitudContactoServicio)
                .crearSolicitud(
                        eq(SALON_ID),
                        eq("Ana"),
                        eq("3001234567"),
                        anyList(),
                        eq(MotivoSolicitud.PEDIDO_POR_IA));

        verify(creditoIaServicio, times(1))
                .consumirCredito(SALON_ID);
    }

    @Test
    void bloqueaPromesaDeDescuentoYRegistraSeguridad() {

        prepararSalonYCatalogo();

        prepararProveedor(
                """
                        {"respuesta":"Te doy un descuento del 30%.",
                         "escalar":false}
                        """);

        when(solicitudContactoServicio.crearSolicitud(
                eq(SALON_ID),
                eq("Ana"),
                eq("3001234567"),
                anyList(),
                eq(MotivoSolicitud.REGLA_DE_SEGURIDAD)))
                .thenReturn(91L);

        RespuestaChatIa respuesta = chatIaServicio.responder(
                SLUG,
                solicitud("Cuanto cuesta el corte?"));

        assertTrue(respuesta.escalado());
        assertEquals(91L, respuesta.solicitudId());

        assertFalse(
                respuesta.respuesta().contains("30%"));

        assertEquals(
                "He registrado su consulta. "
                        + "El equipo del salon se pondra en contacto "
                        + "con usted para brindarle informacion verificada.",
                respuesta.respuesta());

        assertFalse(
                respuesta.respuesta().toLowerCase()
                        .matches("(?s).*\\b(descuent\\w*|cupon\\w*|"
                                + "gratis|gratuit\\w*|promocion\\w*|"
                                + "reembols\\w*|devolucion\\w*).*"));

        verify(solicitudContactoServicio)
                .crearSolicitud(
                        eq(SALON_ID),
                        eq("Ana"),
                        eq("3001234567"),
                        anyList(),
                        eq(MotivoSolicitud.REGLA_DE_SEGURIDAD));

        verify(creditoIaServicio, times(1))
                .consumirCredito(SALON_ID);
    }

    @Test
    void registraEscalamientoSolicitadoPorLaIa() {

        prepararSalonYCatalogo();

        prepararProveedor(
                """
                        {"respuesta":"El equipo puede orientarte.",
                         "escalar":true}
                        """);

        when(solicitudContactoServicio.crearSolicitud(
                eq(SALON_ID),
                eq("Ana"),
                eq("3001234567"),
                anyList(),
                eq(MotivoSolicitud.PEDIDO_POR_IA)))
                .thenReturn(92L);

        RespuestaChatIa respuesta = chatIaServicio.responder(
                SLUG,
                solicitud("Necesito ayuda personalizada"));

        assertTrue(respuesta.escalado());
        assertEquals(92L, respuesta.solicitudId());

        verify(solicitudContactoServicio)
                .crearSolicitud(
                        eq(SALON_ID),
                        eq("Ana"),
                        eq("3001234567"),
                        anyList(),
                        eq(MotivoSolicitud.PEDIDO_POR_IA));

        verify(creditoIaServicio, times(1))
                .consumirCredito(SALON_ID);
    }

    @Test
    void rechazaJsonInvalidoDespuesDeConsumirCredito() {

        prepararSalonYCatalogo();

        String contenido = """
                {"respuesta":"Hola","escalar":"no"}
                """;

        prepararProveedor(contenido);

        assertThrows(
                ExcepcionIntegracion.class,
                () -> chatIaServicio.responder(
                        SLUG,
                        solicitud("Hola")));

        InOrder orden = inOrder(
                creditoIaServicio,
                proveedorIa,
                validadorJsonIa);

        orden.verify(creditoIaServicio)
                .verificarSaldo(SALON_ID);

        orden.verify(proveedorIa)
                .generar(anyString(), anyString());

        orden.verify(creditoIaServicio)
                .consumirCredito(SALON_ID);

        orden.verify(validadorJsonIa)
                .interpretar(contenido);

        orden.verify(validadorJsonIa)
                .obtenerTexto(any(), eq("respuesta"));

        orden.verify(validadorJsonIa)
                .obtenerBooleano(any(), eq("escalar"));

        verify(creditoIaServicio, times(1))
                .consumirCredito(SALON_ID);

        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void proveedorNoDisponibleNoConsumeCreditos() {

        prepararSalonYCatalogo();

        when(proveedorIa.generar(
                anyString(),
                anyString()))
                .thenThrow(
                        new ExcepcionServicioNoDisponible(
                                "Ningun proveedor de IA esta disponible"));

        assertThrows(
                ExcepcionServicioNoDisponible.class,
                () -> chatIaServicio.responder(
                        SLUG,
                        solicitud("Hola")));

        InOrder orden = inOrder(
                creditoIaServicio,
                proveedorIa);

        orden.verify(creditoIaServicio)
                .verificarSaldo(SALON_ID);

        orden.verify(proveedorIa)
                .generar(anyString(), anyString());

        verify(creditoIaServicio, never())
                .consumirCredito(anyLong());

        verifyNoInteractions(validadorJsonIa);
        verifyNoInteractions(solicitudContactoServicio);
    }

    @Test
    void devuelveNoEncontradoParaSlugInexistente() {

        when(salonRepositorio.findBySlug(SLUG))
                .thenReturn(Optional.empty());

        assertThrows(
                ExcepcionRecursoNoEncontrado.class,
                () -> chatIaServicio.responder(
                        SLUG,
                        solicitud("Hola")));

        verifyNoInteractions(
                publicoServicio,
                creditoIaServicio,
                proveedorIa,
                solicitudContactoServicio,
                validadorJsonIa);
    }

    private void prepararSalonYCatalogo() {

        when(salonRepositorio.findBySlug(SLUG))
                .thenReturn(Optional.of(salon));

        when(salon.getId())
                .thenReturn(SALON_ID);

        RespuestaServicioPublico servicio = new RespuestaServicioPublico(
                10L,
                "Corte de cabello",
                "Corte clasico",
                30,
                25000);

        RespuestaCatalogoPublico catalogo = new RespuestaCatalogoPublico(
                "Salon Prueba",
                SLUG,
                "Bogota",
                "Calle 100",
                "3000000000",
                "Lunes a sabado",
                List.of(servicio),
                List.of());

        when(publicoServicio.consultarCatalogo(SLUG))
                .thenReturn(catalogo);
    }

    private void prepararProveedor(String contenido) {

        FacturaIa factura = new FacturaIa(
                "Gemini",
                "gemini-2.5-flash",
                100,
                30,
                BigDecimal.ZERO,
                125L);

        when(proveedorIa.generar(
                anyString(),
                anyString()))
                .thenReturn(
                        new ResultadoProveedorIa(
                                contenido,
                                factura));
    }

    private SolicitudChatIa solicitud(String mensaje) {

        return new SolicitudChatIa(
                "Ana",
                "3001234567",
                List.of(new MensajeHistorial(
                        RolMensaje.CLIENTE,
                        mensaje)));
    }
}

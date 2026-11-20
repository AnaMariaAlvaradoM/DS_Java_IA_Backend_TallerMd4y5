package com.devsenior.sillon.chat;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosIaService;
import com.devsenior.sillon.ia.FacturaIa;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.publico.CatalogoResponse;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRespuesta;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private SalonRepository salones;
    private CatalogoService catalogo;
    private CreditosIaService creditos;
    private BibliotecaPrompts prompts;
    private AsistenteIa ia;
    private SolicitudService solicitudes;
    private ChatService chat;
    private FacturaIa factura;

    @BeforeEach
    void preparar() {
        salones = mock(SalonRepository.class);
        catalogo = mock(CatalogoService.class);
        creditos = mock(CreditosIaService.class);
        prompts = mock(BibliotecaPrompts.class);
        ia = mock(AsistenteIa.class);
        solicitudes = mock(SolicitudService.class);
        chat = new ChatService(salones, catalogo, creditos, prompts, ia,
                new LectorJsonIa(new ObjectMapper()), solicitudes);
        factura = new FacturaIa("Gemini", "modelo", 10, 5, BigDecimal.ZERO, 30);
        Salon salon = new Salon();
        salon.setId(7L);
        when(salones.findBySlug("corte")).thenReturn(Optional.of(salon));
        when(catalogo.obtener("corte")).thenReturn(new CatalogoResponse("Corte", "corte", "Santiago",
                "Calle 1", "123", "Lunes a sábado", List.of(
                new ServicioRespuesta(3L, "Corte", null, 30, 10000, true)), List.of()));
        when(prompts.cargar(eq("chatbot"), anyMap())).thenReturn("instrucciones");
    }

    @Test
    void respondeSinEscalarSiLaIaNoLoSolicita() {
        when(ia.generar(anyString(), anyString()))
                .thenReturn(new RespuestaIa("{\"respuesta\":\"El corte cuesta $10000.\",\"escalar\":false}", factura));
        ChatRespuesta respuesta = chat.responder("corte", solicitud());
        assertEquals("El corte cuesta $10000.", respuesta.respuesta());
        assertFalse(respuesta.escalado());
        assertNull(respuesta.solicitudId());
        assertSame(factura, respuesta.factura());
        verify(creditos).consumir(7L);
        verifyNoInteractions(solicitudes);
    }

    @Test
    void escalaConLosMensajesDelCliente() {
        when(ia.generar(anyString(), anyString()))
                .thenReturn(new RespuestaIa("{\"respuesta\":\"Una persona le ayudará.\",\"escalar\":true}", factura));
        when(solicitudes.crear(anyLong(), anyString(), anyString(), anyString(), any()))
                .thenReturn(91L);
        ChatRespuesta respuesta = chat.responder("corte", solicitud());
        assertTrue(respuesta.escalado());
        assertEquals(91L, respuesta.solicitudId());
        verify(solicitudes).crear(7L, "Ana", "+56912345678", "Quiero un corte", MotivoSolicitud.PEDIDO_POR_IA);
    }

    @Test
    void bloqueaPromocionAunqueLaIaNoPidaEscalar() {
        when(ia.generar(anyString(), anyString()))
                .thenReturn(new RespuestaIa("{\"respuesta\":\"Tiene un descuento del 50%.\",\"escalar\":false}", factura));
        ChatRespuesta respuesta = chat.responder("corte", solicitud());
        assertEquals(ChatService.RESPUESTA_SEGURA, respuesta.respuesta());
        assertTrue(respuesta.escalado());
        verify(solicitudes).crear(7L, "Ana", "+56912345678", "Quiero un corte", MotivoSolicitud.REGLA_DE_SEGURIDAD);
    }

    @Test
    void rechazaJsonInvalidoDespuesDeConsumirCredito() {
        when(ia.generar(anyString(), anyString())).thenReturn(new RespuestaIa("esto no es JSON", factura));
        assertThrows(PasarelaException.class, () -> chat.responder("corte", solicitud()));
        verify(creditos).consumir(7L);
        verifyNoInteractions(solicitudes);
    }

    private ChatSolicitud solicitud() {
        return new ChatSolicitud("Ana", "+56912345678", List.of(new TurnoChat(RolTurno.CLIENTE, "Quiero un corte")));
    }
}

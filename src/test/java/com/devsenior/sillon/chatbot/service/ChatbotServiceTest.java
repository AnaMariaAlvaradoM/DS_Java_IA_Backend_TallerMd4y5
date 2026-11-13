package com.devsenior.sillon.chatbot.service;

import com.devsenior.sillon.chatbot.contacto.model.MotivoSolicitud;
import com.devsenior.sillon.chatbot.contacto.service.SolicitudContactoService;
import com.devsenior.sillon.chatbot.dto.ChatbotResponse;
import com.devsenior.sillon.chatbot.dto.MensajeChat;
import com.devsenior.sillon.chatbot.dto.RespuestaChatbotIA;
import com.devsenior.sillon.compartido.SinCreditosException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.service.CreditoIaService;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIA;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.publico.dto.ConocimientoChatbot;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.repository.SalonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {
    private static final String SLUG = "salon-centro";
    private static final Long SALON_ID = 63L;
    private static final List<MensajeChat> HISTORIAL = List.of(new MensajeChat("CLIENTE", "¿Qué servicios ofrecen?"));
    private static final String RESPUESTA_SEGURA =
            "Para atender su solicitud sobre promociones, descuentos, reembolsos o condiciones especiales, un asesor de nuestro equipo se pondrá en contacto con usted a la brevedad.";

    @Mock private CatalogoService catalogoService;
    @Mock private AsistenteIA asistenteIA;
    @Mock private LectorJsonIA lectorJsonIA;
    @Mock private BibliotecaPrompts bibliotecaPrompts;
    @Mock private SolicitudContactoService solicitudContactoService;
    @Mock private SalonRepository salonRepository;

    private ChatbotService chatbotService;
    private CreditoIaService creditoIaService;
    private RespuestaIA respuestaIA;

    @BeforeEach
    void prepararServicio() {
        creditoIaService = new CreditoIaService(salonRepository);
        chatbotService = new ChatbotService(catalogoService, creditoIaService, asistenteIA,
                lectorJsonIA, bibliotecaPrompts, solicitudContactoService, salonRepository);
        Salon salon = new Salon();
        salon.setId(SALON_ID);
        when(salonRepository.findBySlug(SLUG)).thenReturn(Optional.of(salon));
        when(catalogoService.obtenerConocimientoParaChatbot(SALON_ID))
                .thenReturn(new ConocimientoChatbot("Corte: 30 minutos", "Lunes a sábado"));
        respuestaIA = new RespuestaIA("{}", "mock", "modelo-test", 1, 1, BigDecimal.ZERO, 1L);
    }

    @Test
    void sinCreditosNoLlamaAlMotorDeIA() {
        when(salonRepository.obtenerCreditos(SALON_ID)).thenReturn(0);

        assertThrows(SinCreditosException.class, () -> procesarMensaje());

        verify(asistenteIA, never()).completar(any(String.class), any(String.class));
        verify(salonRepository, never()).consumirUnCredito(SALON_ID);
    }

    @Test
    void preguntaNormalNoEscalaYConsumeUnCredito() {
        prepararRespuestaIA();
        prepararCreditoDisponible();
        when(lectorJsonIA.leer(respuestaIA, RespuestaChatbotIA.class))
                .thenReturn(new RespuestaChatbotIA("Tenemos corte y peinado.", false));

        ChatbotResponse resultado = procesarMensaje();

        assertFalse(resultado.escalado());
        assertNull(resultado.solicitudId());
        assertEquals("Tenemos corte y peinado.", resultado.respuesta());
        verify(salonRepository, times(1)).consumirUnCredito(SALON_ID);
        verify(solicitudContactoService, never()).crearSolicitud(
                any(), any(), any(), any(), any());
    }

    @Test
    void escalamientoSolicitadoPorIaCreaSolicitudYDevuelveSuId() {
        prepararRespuestaIA();
        prepararCreditoDisponible();
        when(lectorJsonIA.leer(respuestaIA, RespuestaChatbotIA.class))
                .thenReturn(new RespuestaChatbotIA("Un asesor le ayudará.", true));
        when(solicitudContactoService.crearSolicitud(
                SLUG, "María", "maria@example.com", "¿Qué servicios ofrecen?", MotivoSolicitud.PEDIDO_POR_IA))
                .thenReturn(solicitud(701L));

        ChatbotResponse resultado = procesarMensaje();

        assertTrue(resultado.escalado());
        assertEquals(701L, resultado.solicitudId());
        verify(solicitudContactoService).crearSolicitud(
                SLUG, "María", "maria@example.com", "¿Qué servicios ofrecen?", MotivoSolicitud.PEDIDO_POR_IA);
    }

    @Test
    void reglaAntiPromesasReemplazaRespuestaYEscalaPorSeguridad() {
        prepararRespuestaIA();
        prepararCreditoDisponible();
        when(lectorJsonIA.leer(respuestaIA, RespuestaChatbotIA.class))
                .thenReturn(new RespuestaChatbotIA("Use el cupón SILLA50 para recibir un descuento gratis.", false));
        when(solicitudContactoService.crearSolicitud(
                SLUG, "María", "maria@example.com", "¿Qué servicios ofrecen?",
                MotivoSolicitud.REGLA_DE_SEGURIDAD))
                .thenReturn(solicitud(702L));

        ChatbotResponse resultado = procesarMensaje();

        assertEquals(RESPUESTA_SEGURA, resultado.respuesta());
        assertTrue(resultado.escalado());
        assertEquals(702L, resultado.solicitudId());
        verify(solicitudContactoService).crearSolicitud(
                eq(SLUG), eq("María"), eq("maria@example.com"),
                eq("¿Qué servicios ofrecen?"), eq(MotivoSolicitud.REGLA_DE_SEGURIDAD));
    }

    private ChatbotResponse procesarMensaje() {
        return chatbotService.procesarMensaje(SLUG, "María", "maria@example.com",
                "Calle 1", HISTORIAL);
    }

    private void prepararRespuestaIA() {
        when(bibliotecaPrompts.cargar(eq("chatbot"), any()))
                .thenReturn("Instrucciones del sistema");
        when(asistenteIA.completar(any(String.class), any(String.class))).thenReturn(respuestaIA);
    }

    private void prepararCreditoDisponible() {
        when(salonRepository.obtenerCreditos(SALON_ID)).thenReturn(1);
        when(salonRepository.consumirUnCredito(SALON_ID)).thenReturn(1);
    }

    private com.devsenior.sillon.chatbot.contacto.model.SolicitudContacto solicitud(Long id) {
        com.devsenior.sillon.chatbot.contacto.model.SolicitudContacto solicitud =
                new com.devsenior.sillon.chatbot.contacto.model.SolicitudContacto();
        solicitud.setId(id);
        return solicitud;
    }
}

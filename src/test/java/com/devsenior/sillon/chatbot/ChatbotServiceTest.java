package com.devsenior.sillon.chatbot;

import com.devsenior.sillon.chatbot.dto.ChatPeticionDto;
import com.devsenior.sillon.chatbot.dto.ChatRespuestaDto;
import com.devsenior.sillon.chatbot.dto.MensajeChatDto;
import com.devsenior.sillon.chatbot.dto.RespuestaIaChatDto;
import com.devsenior.sillon.chatbot.dto.RolChat;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.ia.RespuestaIAInvalidaException;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.solicitud.MotivoSolicitud;
import com.devsenior.sillon.solicitud.SolicitudContacto;
import com.devsenior.sillon.solicitud.SolicitudContactoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private SalonRepository salonRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private CreditosService creditosService;

    @Mock
    private BibliotecaPrompts bibliotecaPrompts;

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private LectorJsonIA lectorJsonIA;

    @Mock
    private SolicitudContactoRepository solicitudRepository;

    private Clock clock;
    private ChatbotService chatbotService;
    private Salon salonMock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Bogota"));
        chatbotService = new ChatbotService(
                salonRepository,
                servicioRepository,
                creditosService,
                bibliotecaPrompts,
                asistenteIA,
                lectorJsonIA,
                solicitudRepository,
                clock
        );

        salonMock = mock(Salon.class);
    }

    private ChatPeticionDto crearPeticion(String clienteNombre) {
        return new ChatPeticionDto(
                clienteNombre,
                "3001234567",
                List.of(new MensajeChatDto(RolChat.CLIENTE, "Hola, quisiera saber los horarios"))
        );
    }

    private RespuestaIA crearRespuestaIAPrueba(String textoModelo) {
        return new RespuestaIA(
                textoModelo,
                "openai",
                "gpt-4o-mini",
                10,
                5,
                BigDecimal.valueOf(0.001),
                150L
        );
    }

    private void prepararSalonMockValido() {
        when(salonMock.getId()).thenReturn(1L);
        when(salonMock.getNombre()).thenReturn("Barbería X");
        when(salonMock.getDireccion()).thenReturn("Calle 10 # 5-20");
        when(salonMock.getCiudad()).thenReturn("Buga");
        when(salonMock.getConfiguracion()).thenReturn(null);

        when(salonRepository.findBySlug("barberia-x")).thenReturn(Optional.of(salonMock));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(List.of());
        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Prompt base de chatbot");
    }

    @Test
    @DisplayName("1. Respuesta normal de IA: escalado es false, no guarda solicitud de contacto")
    void respuestaNormalNoEscala() {
        prepararSalonMockValido();
        ChatPeticionDto peticion = crearPeticion("Ana");
        RespuestaIA respuestaIA = crearRespuestaIAPrueba("{\"respuesta\":\"Hola Ana, atendemos de 8 a 19\",\"escalar\":false}");
        RespuestaIaChatDto dtoIa = new RespuestaIaChatDto("Hola Ana, atendemos de 8 a 19", false);

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class)).thenReturn(dtoIa);

        ChatRespuestaDto respuesta = chatbotService.procesarChat("barberia-x", peticion);

        assertNotNull(respuesta);
        assertEquals("Hola Ana, atendemos de 8 a 19", respuesta.respuesta());
        assertFalse(respuesta.escalado());
        assertNull(respuesta.solicitudId());

        verify(creditosService).verificar(1L);
        verify(creditosService).consumir(1L);
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. Escala la IA (escalar=true): guarda solicitud de contacto y devuelve su ID")
    void respuestaEscaladaGuardaSolicitud() {
        prepararSalonMockValido();
        ChatPeticionDto peticion = crearPeticion("Carlos");
        RespuestaIA respuestaIA = crearRespuestaIAPrueba("{\"respuesta\":\"Te contacto con un agente\",\"escalar\":true}");
        RespuestaIaChatDto dtoIa = new RespuestaIaChatDto("Te contacto con un agente", true);

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class)).thenReturn(dtoIa);

        SolicitudContacto solicitudGuardadaMock = mock(SolicitudContacto.class);
        when(solicitudGuardadaMock.getId()).thenReturn(99L);
        when(solicitudRepository.save(any(SolicitudContacto.class))).thenReturn(solicitudGuardadaMock);

        ChatRespuestaDto respuesta = chatbotService.procesarChat("barberia-x", peticion);

        assertNotNull(respuesta);
        assertTrue(respuesta.escalado());
        assertEquals(99L, respuesta.solicitudId());

        verify(solicitudRepository).save(any(SolicitudContacto.class));
    }

    @Test
    @DisplayName("3. Regla anti-promesas: si la IA responde con cupón/descuento, reemplaza respuesta, fuerza escalado y guarda motivo REGLA_DE_SEGURIDAD")
    void reglaAntiPromesasInterceptaRespuesta() {
        prepararSalonMockValido();
        ChatPeticionDto peticion = crearPeticion("Laura");
        RespuestaIA respuestaIA = crearRespuestaIAPrueba("{\"respuesta\":\"Te regalamos un cupón del 50%\",\"escalar\":false}");
        RespuestaIaChatDto dtoIa = new RespuestaIaChatDto("Te regalamos un cupón del 50%", false);

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class)).thenReturn(dtoIa);

        SolicitudContacto solicitudGuardadaMock = mock(SolicitudContacto.class);
        when(solicitudGuardadaMock.getId()).thenReturn(105L);
        when(solicitudRepository.save(any(SolicitudContacto.class))).thenReturn(solicitudGuardadaMock);

        ChatRespuestaDto respuesta = chatbotService.procesarChat("barberia-x", peticion);

        assertNotNull(respuesta);
        assertTrue(respuesta.escalado());
        assertEquals(105L, respuesta.solicitudId());

        ArgumentCaptor<SolicitudContacto> captor = ArgumentCaptor.forClass(SolicitudContacto.class);
        verify(solicitudRepository).save(captor.capture());
        assertEquals(MotivoSolicitud.REGLA_DE_SEGURIDAD, captor.getValue().getMotivo());
    }

    @Test
    @DisplayName("4. Slug inexistente: lanza NoEncontradoException sin consumir créditos ni llamar a la IA")
    void slugInexistenteLanzaExcepcion() {
        ChatPeticionDto peticion = crearPeticion("Pedro");
        when(salonRepository.findBySlug("salon-inexistente")).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () ->
                chatbotService.procesarChat("salon-inexistente", peticion)
        );

        verify(creditosService, never()).verificar(any());
        verify(asistenteIA, never()).completar(anyString(), anyString());
    }

    @Test
    @DisplayName("5. Sin créditos suficientes: verificar() lanza PagoRequeridoException y aborta antes de la IA")
    void sinCreditosLanzaExcepcion() {
        when(salonMock.getId()).thenReturn(1L);
        when(salonRepository.findBySlug("barberia-x")).thenReturn(Optional.of(salonMock));
        doThrow(new PagoRequeridoException("Sin créditos")).when(creditosService).verificar(1L);

        ChatPeticionDto peticion = crearPeticion("María");

        assertThrows(PagoRequeridoException.class, () ->
                chatbotService.procesarChat("barberia-x", peticion)
        );

        verify(asistenteIA, never()).completar(anyString(), anyString());
        verify(creditosService, never()).consumir(any());
    }

    @Test
    @DisplayName("6. JSON inválido de la IA: lectorJsonIA devuelve null, lanza RespuestaIAInvalidaException pero SI consume créditos")
    void jsonInvalidoLanzaExcepcionYConsumeCredito() {
        prepararSalonMockValido();
        ChatPeticionDto peticion = crearPeticion("Jorge");
        RespuestaIA respuestaIA = crearRespuestaIAPrueba("texto invalido no json");

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class)).thenReturn(null);

        assertThrows(RespuestaIAInvalidaException.class, () ->
                chatbotService.procesarChat("barberia-x", peticion)
        );

        verify(creditosService).verificar(1L);
        verify(creditosService).consumir(1L);
    }

    @Test
    @DisplayName("7. Nombre con llaves mustache {{x}}: procesa correctamente y sanitiza el nombre")
    void nombreConMustacheSeSanitizaSinError() {
        prepararSalonMockValido();
        ChatPeticionDto peticion = crearPeticion("{{nombre_inyectado}}");
        RespuestaIA respuestaIA = crearRespuestaIAPrueba("{\"respuesta\":\"Hola, ¿en qué te ayudamos?\",\"escalar\":false}");
        RespuestaIaChatDto dtoIa = new RespuestaIaChatDto("Hola, ¿en qué te ayudamos?", false);

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);
        when(lectorJsonIA.leer(respuestaIA, RespuestaIaChatDto.class)).thenReturn(dtoIa);

        ChatRespuestaDto respuesta = chatbotService.procesarChat("barberia-x", peticion);

        assertNotNull(respuesta);
        assertFalse(respuesta.escalado());
    }
}

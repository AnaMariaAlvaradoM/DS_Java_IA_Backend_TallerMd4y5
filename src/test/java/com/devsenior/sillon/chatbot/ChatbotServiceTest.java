package com.devsenior.sillon.chatbot;

import com.devsenior.sillon.chatbot.dto.ChatPeticionDto;
import com.devsenior.sillon.chatbot.dto.ChatRespuestaDto;
import com.devsenior.sillon.chatbot.dto.MensajeChatDto;
import com.devsenior.sillon.chatbot.dto.RespuestaIaChatDto;
import com.devsenior.sillon.chatbot.dto.RolChat;
import com.devsenior.sillon.compartido.NoEncontradoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.salon.Salon;
import com.devsenior.sillon.salon.SalonRepository;
import com.devsenior.sillon.servicio.ServicioRepository;
import com.devsenior.sillon.solicitud.SolicitudContactoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    @Mock
    private Clock clock;

    @InjectMocks
    private ChatbotService chatbotService;

    private Salon salonSimulado;

    @BeforeEach
    void setUp() {
        salonSimulado = new Salon();
        salonSimulado.setId(1L);
        salonSimulado.setNombre("Salón Prueba");
        salonSimulado.setSlug("salon-prueba");
    }

    @Test
    void procesarChat_SalonNoExistente_LanzaNoEncontradoException() {
        when(salonRepository.findBySlug("no-existe")).thenReturn(Optional.empty());

        ChatPeticionDto peticion = new ChatPeticionDto("Juan", "juan@test.com", Collections.emptyList());

        assertThrows(NoEncontradoException.class, () -> chatbotService.procesarChat("no-existe", peticion));
    }

    @Test
    void procesarChat_FlujoExitoso_RetornaRespuestaDto() {
        when(salonRepository.findBySlug("salon-prueba")).thenReturn(Optional.of(salonSimulado));
        when(servicioRepository.findBySalonIdAndActivoTrue(1L)).thenReturn(Collections.emptyList());
        when(bibliotecaPrompts.cargar(anyString(), anyMap())).thenReturn("Instrucciones de prueba");

        String jsonSimulado = """
                {"respuesta": "Hola, ¿en qué te ayudo?", "escalar": false}
                """;

        RespuestaIA respuestaIA = new RespuestaIA(
                jsonSimulado,
                "gemini",
                "gemini-3.5-flash-lite",
                10,
                20,
                BigDecimal.ZERO,
                150L
        );

        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIA);

        RespuestaIaChatDto respuestaIaDto = new RespuestaIaChatDto("Hola, ¿en qué te ayudo?", false);
        when(lectorJsonIA.leer(any(RespuestaIA.class), eq(RespuestaIaChatDto.class))).thenReturn(respuestaIaDto);

        ChatPeticionDto peticion = new ChatPeticionDto(
                "Carlos",
                "carlos@email.com",
                List.of(new MensajeChatDto(RolChat.CLIENTE, "Hola"))
        );

        ChatRespuestaDto resultado = chatbotService.procesarChat("salon-prueba", peticion);

        assertNotNull(resultado);
        assertEquals("Hola, ¿en qué te ayudo?", resultado.respuesta());
        assertFalse(resultado.escalado());

        verify(creditosService).verificar(1L);
        verify(creditosService).consumir(1L);
    }
}
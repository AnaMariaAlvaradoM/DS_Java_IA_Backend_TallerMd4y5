package com.devsenior.sillon.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.ChatResponse;
import com.devsenior.sillon.chat.dto.TurnoChat;
import com.devsenior.sillon.chat.model.MotivoSolicitud;
import com.devsenior.sillon.chat.model.RolTurno;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.ia.excepcion.RespuestaIAInvalidaException;
import com.devsenior.sillon.ia.model.RespuestaIA;
import com.devsenior.sillon.ia.service.AsistenteIA;
import com.devsenior.sillon.ia.service.BibliotecaPrompts;
import com.devsenior.sillon.ia.service.LectorJsonIA;
import com.devsenior.sillon.publico.service.CatalogoService;
import com.devsenior.sillon.salon.model.ConfiguracionSalon;
import com.devsenior.sillon.salon.model.Salon;
import com.devsenior.sillon.salon.service.CreditosService;
import com.devsenior.sillon.salon.service.SalonService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final String SLUG = "barberia-la-navaja";

    @Mock
    private SalonService salonService;
    @Mock
    private CatalogoService catalogoService;
    @Mock
    private CreditosService creditosService;
    @Mock
    private AsistenteIA asistenteIA;
    @Mock
    private SolicitudService solicitudService;

    private ChatbotService chatbotService;
    private Salon salon;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(salonService, catalogoService, creditosService, new BibliotecaPrompts(),
                asistenteIA, new LectorJsonIA(JsonMapper.builder().build()), solicitudService);
        salon = new Salon();
        salon.setId(1L);
        salon.setNombre("Barbería La Navaja");
        salon.setDireccion("Calle 10 # 43-20");
        salon.setCiudad("Medellín");
        salon.setConfiguracion(ConfiguracionSalon.porDefecto());
        lenient().when(salonService.obtenerPorSlug(SLUG)).thenReturn(salon);
    }

    @Test
    void conversar_sinCreditos_lanza402SinLlamarALaIA() {
        doThrow(new PagoRequeridoException("sin créditos")).when(creditosService).verificar(1L);

        assertThrows(PagoRequeridoException.class, () -> chatbotService.conversar(SLUG, pregunta()));
        verify(asistenteIA, never()).completar(anyString(), anyString());
        verify(creditosService, never()).consumir(anyLong());
    }

    @Test
    void conversar_preguntaNormal_respondeSinEscalarYConsumeUnCredito() {
        prepararIA("{\"respuesta\":\"El corte clásico cuesta $35.000 y dura 45 minutos.\",\"escalar\":false}");

        ChatResponse respuesta = chatbotService.conversar(SLUG, pregunta());

        assertFalse(respuesta.escalado());
        assertNull(respuesta.solicitudId());
        assertTrue(respuesta.respuesta().contains("35.000"));
        verify(creditosService).consumir(1L);
        verify(solicitudService, never()).crear(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void conversar_laIAPideEscalar_creaSolicitudSoloConLosMensajesDelCliente() {
        prepararIA("{\"respuesta\":\"Con gusto. Una persona del salón lo contactará para confirmar.\",\"escalar\":true}");
        when(solicitudService.crear(eq(1L), eq("Juliana"), eq("315 444 5566"),
                eq("Hola, ¿cuánto cuesta el corte clásico?\nPerfecto, quiero reservar para el sábado en la mañana"),
                eq(MotivoSolicitud.PEDIDO_POR_IA))).thenReturn(7L);

        ChatResponse respuesta = chatbotService.conversar(SLUG, reserva());

        assertTrue(respuesta.escalado());
        assertEquals(7L, respuesta.solicitudId());
    }

    @Test
    void conversar_laIAPrometeUnCupon_elClienteRecibeElMensajeSeguroYSeEscalaPorSeguridad() {
        prepararIA("{\"respuesta\":\"Claro, su cupón SILLA50 le da 50% de descuento en su próxima cita.\",\"escalar\":false}");
        when(solicitudService.crear(eq(1L), anyString(), anyString(), anyString(),
                eq(MotivoSolicitud.REGLA_DE_SEGURIDAD))).thenReturn(8L);

        ChatResponse respuesta = chatbotService.conversar(SLUG, pregunta());

        assertEquals(ChatbotService.MENSAJE_SEGURO, respuesta.respuesta());
        assertTrue(respuesta.escalado());
        assertEquals(8L, respuesta.solicitudId());
        assertFalse(respuesta.respuesta().toLowerCase().matches(".*(cup[oó]n|descuento|gratis|silla50).*"));
    }

    @Test
    void conversar_jsonSinLaClaveEscalar_lanza502() {
        prepararIA("{\"respuesta\":\"Hola\"}");

        assertThrows(RespuestaIAInvalidaException.class, () -> chatbotService.conversar(SLUG, pregunta()));
    }

    @Test
    void conversar_elHistorialVaComoMensajeYElCatalogoEnLasInstrucciones() {
        prepararIA("{\"respuesta\":\"Hola\",\"escalar\":false}");

        chatbotService.conversar(SLUG, reserva());

        ArgumentCaptor<String> instrucciones = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(asistenteIA).completar(instrucciones.capture(), mensaje.capture());
        assertTrue(instrucciones.getValue().contains("\"escalar\""));
        assertTrue(instrucciones.getValue().contains("respuesta"));
        assertTrue(instrucciones.getValue().contains("- Corte clásico: 45 minutos, $35.000"));
        assertFalse(instrucciones.getValue().contains("quiero reservar"), "el texto del cliente no va en las instrucciones");
        assertEquals("Cliente: Hola, ¿cuánto cuesta el corte clásico?\n"
                + "Asistente: El corte clásico cuesta $35.000 y dura 45 minutos.\n"
                + "Cliente: Perfecto, quiero reservar para el sábado en la mañana", mensaje.getValue());
    }

    @ParameterizedTest(name = "\"{0}\" → promesa: {1}")
    @CsvSource({
            "'Tenemos un DESCUENTO especial', true",
            "'Le regalo un cupon', true",
            "'La primera cita es gratis', true",
            "'Hay una Promoción este mes', true",
            "'Le hacemos el reembolso', true",
            "'El corte clásico cuesta $35.000', false"
    })
    void contienePromesa_detectaLasPalabrasProhibidas(String texto, boolean esperado) {
        assertEquals(esperado, ChatbotService.contienePromesa(texto));
    }

    private void prepararIA(String json) {
        when(catalogoService.textoCatalogo(1L)).thenReturn("- Corte clásico: 45 minutos, $35.000");
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(new RespuestaIA(json, "openai", "gemini", 400, 40, BigDecimal.ZERO, 500));
    }

    private ChatRequest pregunta() {
        return new ChatRequest("Juliana", "315 444 5566",
                List.of(new TurnoChat(RolTurno.CLIENTE, "Hola, ¿cuánto cuesta el corte clásico y cuánto dura?")));
    }

    private ChatRequest reserva() {
        return new ChatRequest("Juliana", "315 444 5566", List.of(
                new TurnoChat(RolTurno.CLIENTE, "Hola, ¿cuánto cuesta el corte clásico?"),
                new TurnoChat(RolTurno.ASISTENTE, "El corte clásico cuesta $35.000 y dura 45 minutos."),
                new TurnoChat(RolTurno.CLIENTE, "Perfecto, quiero reservar para el sábado en la mañana")));
    }
}

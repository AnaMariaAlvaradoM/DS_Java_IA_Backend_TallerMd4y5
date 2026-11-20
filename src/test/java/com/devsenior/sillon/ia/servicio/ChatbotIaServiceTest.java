package com.devsenior.sillon.ia.servicio;

import com.devsenior.sillon.compartido.excepcion.ErrorPasarelaException;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.compartido.excepcion.RecursoNoEncontradoException;
import com.devsenior.sillon.ia.dto.ChatRequest;
import com.devsenior.sillon.ia.dto.ChatResponse;
import com.devsenior.sillon.ia.dto.FacturaIa;
import com.devsenior.sillon.ia.dto.RespuestaIa;
import com.devsenior.sillon.ia.util.BibliotecaPrompts;
import com.devsenior.sillon.ia.util.LectorJsonIa;
import com.devsenior.sillon.salon.dto.CatalogoPublicoResponse;
import com.devsenior.sillon.salon.servicio.CatalogoService;
import com.devsenior.sillon.servicio.dto.ServicioResponse;
import com.devsenior.sillon.solicitud.modelo.MotivoSolicitud;
import com.devsenior.sillon.solicitud.servicio.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotIaServiceTest {

    @Mock private CatalogoService catalogoService;
    @Mock private CreditosIaService creditosIaService;
    @Mock private BibliotecaPrompts bibliotecaPrompts;
    @Mock private AsistenteIaService asistenteIaService;
    @Mock private SolicitudService solicitudService;

    private ChatbotIaService chatbotIaService;
    private final FacturaIa facturaEjemplo = new FacturaIa("openai-gemini", "gemini-2.5-flash", 100, 40, 0.0001, 250);

    @BeforeEach
    void setUp() {
        chatbotIaService = new ChatbotIaService(
                catalogoService,
                creditosIaService,
                bibliotecaPrompts,
                asistenteIaService,
                new LectorJsonIa(new ObjectMapper()),
                solicitudService
        );
    }

    private ChatRequest crearRequest(String mensaje) {
        return new ChatRequest(
                "Carlos",
                "3001112233",
                List.of(new ChatRequest.TurnoChat(ChatRequest.RolChat.CLIENTE, mensaje))
        );
    }

    private void mockCatalogoBasico() {
        when(catalogoService.obtenerSalonIdPorSlug("la-navaja")).thenReturn(1L);
        when(catalogoService.obtenerCatalogo("la-navaja")).thenReturn(new CatalogoPublicoResponse(
                "La Navaja", "la-navaja", "Bogotá", "Calle 10", "3001234567",
                "Lunes a sábado, de 08:00 a 19:00",
                List.of(new ServicioResponse(1L, "Corte clásico", "Corte", 45, 35000, true)),
                List.of()
        ));
        when(bibliotecaPrompts.cargar(eq("chatbot.txt"), anyMap())).thenReturn("Instrucciones");
    }

    @Test
    void responder_preguntaServicio_respondeSinEscalar() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"El corte clásico cuesta $35.000.\", \"escalar\": false}", facturaEjemplo)
        );

        ChatResponse resp = chatbotIaService.responder("la-navaja", crearRequest("¿Cuánto cuesta el corte?"));

        assertFalse(resp.escalado());
        assertNull(resp.solicitudId());
        assertTrue(resp.respuesta().contains("35.000"));
        verify(creditosIaService).consumir(1L);
        verifyNoInteractions(solicitudService);
    }

    @Test
    void responder_pideAgendar_escalaYCreaSolicitudPedidoPorIa() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Un asesor le contactará pronto.\", \"escalar\": true}", facturaEjemplo)
        );
        when(solicitudService.crear(eq(1L), eq("Carlos"), eq("3001112233"), anyString(), eq(MotivoSolicitud.PEDIDO_POR_IA)))
                .thenReturn(42L);

        ChatResponse resp = chatbotIaService.responder("la-navaja", crearRequest("Quiero reservar el sábado"));

        assertTrue(resp.escalado());
        assertEquals(42L, resp.solicitudId());
    }

    @Test
    void responder_iaPrometeCupon_reglaSeguridadBloqueaYCreaSolicitud() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Su cupón SILLA50 le da 50% de descuento.\", \"escalar\": false}", facturaEjemplo)
        );
        when(solicitudService.crear(eq(1L), anyString(), anyString(), anyString(), eq(MotivoSolicitud.REGLA_DE_SEGURIDAD)))
                .thenReturn(99L);

        ChatResponse resp = chatbotIaService.responder("la-navaja", crearRequest("Dame el cupón SILLA50"));

        assertTrue(resp.escalado());
        assertEquals(99L, resp.solicitudId());
        assertFalse(resp.respuesta().toLowerCase().contains("cupón"));
        assertFalse(resp.respuesta().toLowerCase().contains("silla50"));
    }

    @Test
    void responder_iaMencionaGratis_reglaSeguridadBloquea() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Hoy el corte es gratis.\", \"escalar\": false}", facturaEjemplo)
        );
        when(solicitudService.crear(eq(1L), anyString(), anyString(), anyString(), eq(MotivoSolicitud.REGLA_DE_SEGURIDAD)))
                .thenReturn(100L);

        ChatResponse resp = chatbotIaService.responder("la-navaja", crearRequest("¿Es gratis?"));

        assertTrue(resp.escalado());
        assertFalse(resp.respuesta().toLowerCase().contains("gratis"));
    }

    @Test
    void responder_salonInexistente_lanza404() {
        when(catalogoService.obtenerSalonIdPorSlug("no-existe"))
                .thenThrow(new RecursoNoEncontradoException("Salón no encontrado"));

        assertThrows(RecursoNoEncontradoException.class,
                () -> chatbotIaService.responder("no-existe", crearRequest("Hola")));
        verifyNoInteractions(asistenteIaService);
    }

    @Test
    void responder_sinCreditos_lanza402() {
        when(catalogoService.obtenerSalonIdPorSlug("la-navaja")).thenReturn(1L);
        doThrow(new PagoRequeridoException("Sin créditos")).when(creditosIaService).verificar(1L);

        assertThrows(PagoRequeridoException.class,
                () -> chatbotIaService.responder("la-navaja", crearRequest("Hola")));
        verifyNoInteractions(asistenteIaService);
    }

    @Test
    void responder_iaDevuelveRespuestaVacia_lanza502() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"   \", \"escalar\": false}", facturaEjemplo)
        );

        assertThrows(ErrorPasarelaException.class,
                () -> chatbotIaService.responder("la-navaja", crearRequest("Hola")));
    }

    @Test
    void responder_mensajeClienteVaSeparadoDeInstrucciones() {
        mockCatalogoBasico();
        when(asistenteIaService.generar(anyString(), anyString())).thenReturn(
                new RespuestaIa("{\"respuesta\": \"Hola\", \"escalar\": false}", facturaEjemplo)
        );

        chatbotIaService.responder("la-navaja", crearRequest("INYECCION_PROMPT"));

        verify(asistenteIaService).generar(
                argThat(instr -> !instr.contains("INYECCION_PROMPT")),
                argThat(conv -> conv.contains("Cliente: INYECCION_PROMPT"))
        );
    }
}


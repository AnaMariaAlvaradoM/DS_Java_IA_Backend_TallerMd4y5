package com.devsenior.sillon.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.ia.AsistenteIA;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditosService;
import com.devsenior.sillon.ia.LectorJsonIA;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.publico.SalonPublicoRespuesta;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final String SLUG = "mi-salon";
    private static final Long SALON_ID = 1L;

    @Mock
    private CatalogoService catalogoService;
    @Mock
    private CreditosService creditosService;
    @Mock
    private AsistenteIA asistenteIA;
    @Mock
    private BibliotecaPrompts bibliotecaPrompts;
    @Mock
    private LectorJsonIA lectorJsonIA;
    @Mock
    private SolicitudContactoService solicitudContactoService;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(catalogoService, creditosService, asistenteIA, bibliotecaPrompts,
                lectorJsonIA, solicitudContactoService);
    }

    private SalonPublicoRespuesta salonPublico() {
        return new SalonPublicoRespuesta(SALON_ID, "Mi Salon", SLUG, "Bogota", "Calle 1", "3000000000",
                "8:00-19:00", List.of(), List.of());
    }

    private ChatRequest request() {
        return new ChatRequest("Cliente Uno", "3001112222",
                List.of(new TurnoChat(RolTurno.CLIENTE, "Hola, quiero un corte")));
    }

    private RespuestaIA respuestaIaConTexto(String texto) {
        return new RespuestaIA(texto, "openai", "gemini-3.5-flash-lite", 10, 10, BigDecimal.ZERO, 100L);
    }

    @Test
    void sinCreditosNoLlamaALaIaYDaPagoRequerido() {
        when(catalogoService.obtenerCatalogo(SLUG)).thenReturn(salonPublico());
        doThrow(new PagoRequeridoException("sin creditos")).when(creditosService).verificar(SALON_ID);

        assertThatThrownBy(() -> chatbotService.chatear(SLUG, request()))
                .isInstanceOf(PagoRequeridoException.class);

        verify(asistenteIA, never()).completar(any(), any());
    }

    @Test
    void preguntaNormalNoEscalaYConsumeUnCredito() {
        when(catalogoService.obtenerCatalogo(SLUG)).thenReturn(salonPublico());
        when(bibliotecaPrompts.cargar(eq("chatbot"), any())).thenReturn("instrucciones");
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuestaIaConTexto("{\"respuesta\":\"Claro, tenemos turnos\",\"escalar\":false}"));
        when(lectorJsonIA.leer(any(), eq(ChatIaRespuesta.class)))
                .thenReturn(new ChatIaRespuesta("Claro, tenemos turnos", false));

        ChatRespuesta respuesta = chatbotService.chatear(SLUG, request());

        assertThat(respuesta.escalado()).isFalse();
        assertThat(respuesta.solicitudId()).isNull();
        assertThat(respuesta.respuesta()).isEqualTo("Claro, tenemos turnos");
        verify(creditosService).consumir(SALON_ID);
        verify(solicitudContactoService, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void laIaPideEscalarYSeCreaLaSolicitudConMotivoPedidoPorIa() {
        when(catalogoService.obtenerCatalogo(SLUG)).thenReturn(salonPublico());
        when(bibliotecaPrompts.cargar(eq("chatbot"), any())).thenReturn("instrucciones");
        when(asistenteIA.completar(anyString(), anyString()))
                .thenReturn(respuestaIaConTexto("{\"respuesta\":\"Te comunico con el salon\",\"escalar\":true}"));
        when(lectorJsonIA.leer(any(), eq(ChatIaRespuesta.class)))
                .thenReturn(new ChatIaRespuesta("Te comunico con el salon", true));
        when(solicitudContactoService.crear(eq(SALON_ID), any(), any(), any(), eq(MotivoSolicitud.PEDIDO_POR_IA)))
                .thenReturn(55L);

        ChatRespuesta respuesta = chatbotService.chatear(SLUG, request());

        assertThat(respuesta.escalado()).isTrue();
        assertThat(respuesta.solicitudId()).isEqualTo(55L);
        assertThat(respuesta.respuesta()).isEqualTo("Te comunico con el salon");
        verify(solicitudContactoService).crear(eq(SALON_ID), any(), any(), any(), eq(MotivoSolicitud.PEDIDO_POR_IA));
    }

    @ParameterizedTest
    @ValueSource(strings = { "descuento", "DESCUENTO", "cupón", "CUPÓN", "cupon", "CUPON", "gratis", "GRATIS",
            "promoción", "PROMOCIÓN", "promocion", "PROMOCION", "reembols", "REEMBOLS" })
    void laIaPrometiendoAlgoProhibidoActivaLaRespuestaSegura(String palabraProhibida) {
        when(catalogoService.obtenerCatalogo(SLUG)).thenReturn(salonPublico());
        when(bibliotecaPrompts.cargar(eq("chatbot"), any())).thenReturn("instrucciones");
        String textoIa = "Te puedo ofrecer un " + palabraProhibida + " especial";
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIaConTexto(textoIa));
        when(lectorJsonIA.leer(any(), eq(ChatIaRespuesta.class))).thenReturn(new ChatIaRespuesta(textoIa, false));
        when(solicitudContactoService.crear(eq(SALON_ID), any(), any(), any(),
                eq(MotivoSolicitud.REGLA_DE_SEGURIDAD))).thenReturn(77L);

        ChatRespuesta respuesta = chatbotService.chatear(SLUG, request());

        assertThat(respuesta.escalado()).isTrue();
        assertThat(respuesta.solicitudId()).isEqualTo(77L);
        assertThat(respuesta.respuesta())
                .isEqualTo("Gracias por tu mensaje. Una persona de nuestro equipo te va a contactar pronto "
                        + "para ayudarte con esto.");
        verify(solicitudContactoService).crear(eq(SALON_ID), any(), any(), any(),
                eq(MotivoSolicitud.REGLA_DE_SEGURIDAD));
    }

    @Test
    void elMensajeSeguroNoContieneNingunaPalabraProhibida() {
        when(catalogoService.obtenerCatalogo(SLUG)).thenReturn(salonPublico());
        when(bibliotecaPrompts.cargar(eq("chatbot"), any())).thenReturn("instrucciones");
        String textoIa = "Aprovecha este descuento especial";
        when(asistenteIA.completar(anyString(), anyString())).thenReturn(respuestaIaConTexto(textoIa));
        when(lectorJsonIA.leer(any(), eq(ChatIaRespuesta.class))).thenReturn(new ChatIaRespuesta(textoIa, false));
        when(solicitudContactoService.crear(any(), any(), any(), any(), any())).thenReturn(1L);

        ChatRespuesta respuesta = chatbotService.chatear(SLUG, request());

        List<String> palabrasProhibidas = List.of("descuento", "cupón", "cupon", "gratis", "promoción", "promocion",
                "reembols");
        String respuestaMinuscula = respuesta.respuesta().toLowerCase(Locale.ROOT);
        for (String palabra : palabrasProhibidas) {
            assertThat(respuestaMinuscula).doesNotContain(palabra);
        }
    }
}

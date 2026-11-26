package com.devsenior.sillon.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.Datos;
import com.devsenior.sillon.compartido.PagoRequeridoException;
import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.CreditoService;
import com.devsenior.sillon.ia.LectorJson;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.publico.CatalogoService;
import com.devsenior.sillon.salon.Plan;

/** Tests del chatbot: la IA está simulada con Mockito, ningún test llama a Gemini. */
class ChatbotServiceTest {

    private final CatalogoService catalogoService = mock(CatalogoService.class);
    private final CreditoService creditoService = mock(CreditoService.class);
    private final BibliotecaPrompts bibliotecaPrompts = mock(BibliotecaPrompts.class);
    private final AsistenteIa asistenteIa = mock(AsistenteIa.class);
    private final SolicitudService solicitudService = mock(SolicitudService.class);
    private ChatbotService chatbot;

    private final ChatPeticion pregunta = new ChatPeticion("Juliana", "315 444 5566",
            List.of(new Turno(RolTurno.CLIENTE, "¿Cuánto cuesta el corte clásico?")));

    @BeforeEach
    void preparar() {
        chatbot = new ChatbotService(catalogoService, creditoService, bibliotecaPrompts, asistenteIa, new LectorJson(),
                solicitudService);
        when(catalogoService.salonPorSlug("barberia-la-navaja")).thenReturn(Datos.salon(Plan.GRATIS));
        when(catalogoService.catalogoTexto(1L)).thenReturn("- Corte clásico: 45 minutos, $35.000");
        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("instrucciones");
        when(solicitudService.crear(eq(1L), anyString(), anyString(), anyString(), any())).thenReturn(77L);
    }

    private void iaResponde(String json) {
        when(asistenteIa.generar(anyString(), anyString()))
                .thenReturn(new RespuestaIa(json, "gemini", "gemini-2.5-flash", 120, 30, 400, 0.0001));
    }

    @Test
    void sinCreditosNoSeLlamaALaIa() {
        doThrow(new PagoRequeridoException("sin créditos")).when(creditoService).verificar(1L);

        assertThatThrownBy(() -> chatbot.responder("barberia-la-navaja", pregunta))
                .isInstanceOf(PagoRequeridoException.class);
        verify(asistenteIa, never()).generar(anyString(), anyString());
    }

    @Test
    void preguntaNormalNoEscalaYConsumeUnCredito() {
        iaResponde("{\"respuesta\":\"El corte clásico cuesta $35.000 y dura 45 minutos.\",\"escalar\":false}");

        ChatRespuesta r = chatbot.responder("barberia-la-navaja", pregunta);

        assertThat(r.escalado()).isFalse();
        assertThat(r.solicitudId()).isNull();
        assertThat(r.factura().proveedor()).isEqualTo("gemini");
        verify(creditoService).consumir(1L);
        verify(solicitudService, never()).crear(any(), any(), any(), any(), any());
    }

    @Test
    void cuandoLaIaPideEscalarSeCreaLaSolicitud() {
        iaResponde("{\"respuesta\":\"Una persona del salón lo contactará.\",\"escalar\":true}");

        ChatRespuesta r = chatbot.responder("barberia-la-navaja", pregunta);

        assertThat(r.escalado()).isTrue();
        assertThat(r.solicitudId()).isEqualTo(77L);
        verify(solicitudService).crear(eq(1L), eq("Juliana"), eq("315 444 5566"), anyString(),
                eq(MotivoSolicitud.PEDIDO_POR_IA));
    }

    @Test
    void siLaIaPrometeUnCuponElClienteRecibeElMensajeSeguro() {
        iaResponde("{\"respuesta\":\"Claro, su cupón SILLA50 le da 50% de descuento.\",\"escalar\":false}");

        ChatRespuesta r = chatbot.responder("barberia-la-navaja", pregunta);

        assertThat(r.respuesta()).isEqualTo(ChatbotService.MENSAJE_SEGURO);
        assertThat(r.respuesta()).doesNotContainIgnoringCase("cupón").doesNotContain("SILLA50");
        assertThat(r.escalado()).isTrue();
        verify(solicitudService).crear(eq(1L), anyString(), anyString(), anyString(),
                eq(MotivoSolicitud.REGLA_DE_SEGURIDAD));
    }

    @Test
    void respuestaDeLaIaQueNoEsJsonDa502() {
        iaResponde("Hola, soy un texto sin formato");

        assertThatThrownBy(() -> chatbot.responder("barberia-la-navaja", pregunta))
                .isInstanceOf(PasarelaException.class);
    }

    @Test
    void laReglaAntiPromesasDetectaTodasLasPalabras() {
        assertThat(ChatbotService.contienePromesa("Tenemos un DESCUENTO")).isTrue();
        assertThat(ChatbotService.contienePromesa("le damos un cupon")).isTrue();
        assertThat(ChatbotService.contienePromesa("el lavado es gratis")).isTrue();
        assertThat(ChatbotService.contienePromesa("hay promoción")).isTrue();
        assertThat(ChatbotService.contienePromesa("le hacemos el reembolso")).isTrue();
        assertThat(ChatbotService.contienePromesa("El corte cuesta $35.000")).isFalse();
    }
}

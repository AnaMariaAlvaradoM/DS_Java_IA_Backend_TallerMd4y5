package com.devsenior.sillon.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.sillon.chat.dto.ChatRequest;
import com.devsenior.sillon.chat.dto.TurnoChat;
import com.devsenior.sillon.compartido.excepcion.PagoRequeridoException;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.FacturaIa;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.RespuestaIa;
import com.devsenior.sillon.publico.ConocimientoChat;
import com.devsenior.sillon.publico.PublicoService;
import com.devsenior.sillon.salon.CreditosIaService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock private PublicoService catalogo;
    @Mock private CreditosIaService creditos;
    @Mock private AsistenteIa asistente;
    @Mock private SolicitudContactoService solicitudes;

    private ChatService chatbot;
    private final ChatRequest peticion = new ChatRequest("Sara", "3001234567",
            List.of(new TurnoChat("CLIENTE", "Cuanto cuesta el corte")));

    @BeforeEach
    void preparar() {
        given(catalogo.conocimientoChat("salon")).willReturn(new ConocimientoChat(1L, "Salon", "Calle 1",
                "08:00 a 19:00", "Corte (45 minutos): $35.000"));
        chatbot = new ChatService(catalogo, creditos, new BibliotecaPrompts(), asistente,
                new LectorJsonIa(new ObjectMapper()), solicitudes);
    }

    @Test
    void sinCreditosNoLlamaALaIa() {
        org.mockito.Mockito.doThrow(new PagoRequeridoException("No quedan créditos")).when(creditos).verificar(1L);

        assertThatThrownBy(() -> chatbot.responder("salon", peticion)).isInstanceOf(PagoRequeridoException.class);

        verifyNoInteractions(asistente, solicitudes);
    }

    @Test
    void preguntaNormalNoEscalaYConsumeUnCredito() {
        responderCon("{\"respuesta\":\"El corte cuesta $35.000.\",\"escalar\":false}");

        var respuesta = chatbot.responder("salon", peticion);

        assertThat(respuesta.respuesta()).isEqualTo("El corte cuesta $35.000.");
        assertThat(respuesta.escalado()).isFalse();
        assertThat(respuesta.solicitudId()).isNull();
        verify(creditos).consumir(1L);
        verifyNoInteractions(solicitudes);
    }

    @Test
    void iaPideEscalarYCreaSolicitudConMensajesDelCliente() {
        responderCon("{\"respuesta\":\"Una persona lo contactará.\",\"escalar\":true}");
        given(solicitudes.crear(1L, "Sara", "3001234567", "Cuanto cuesta el corte", MotivoSolicitud.PEDIDO_POR_IA))
                .willReturn(2L);

        var respuesta = chatbot.responder("salon", peticion);

        assertThat(respuesta.escalado()).isTrue();
        assertThat(respuesta.solicitudId()).isEqualTo(2L);
        verify(creditos).consumir(1L);
        verify(solicitudes).crear(1L, "Sara", "3001234567", "Cuanto cuesta el corte", MotivoSolicitud.PEDIDO_POR_IA);
    }

    @Test
    void promesaDeCuponSeReemplazaPorMensajeSeguroYEscalaAunqueLaIaDigaQueNo() {
        responderCon("{\"respuesta\":\"Su cupón SILLA50 le da 50% de descuento.\",\"escalar\":false}");
        given(solicitudes.crear(1L, "Sara", "3001234567", "Cuanto cuesta el corte", MotivoSolicitud.REGLA_DE_SEGURIDAD))
                .willReturn(3L);

        var respuesta = chatbot.responder("salon", peticion);

        assertThat(respuesta.respuesta()).contains("se pondrá en contacto")
                .doesNotContain("cupón", "SILLA50", "descuento");
        assertThat(respuesta.escalado()).isTrue();
        assertThat(respuesta.solicitudId()).isEqualTo(3L);
        verify(creditos).consumir(1L);
        verify(solicitudes).crear(1L, "Sara", "3001234567", "Cuanto cuesta el corte", MotivoSolicitud.REGLA_DE_SEGURIDAD);
    }

    private void responderCon(String texto) {
        given(asistente.responder(anyString(), anyString())).willReturn(
                new RespuestaIa(texto, new FacturaIa("Simulado", "simulado", 10, 5, BigDecimal.ZERO, 1)));
    }
}

package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.ObjectMapper;

class MotorIaTest {

    @Test
    void usaElPrimerProveedorDisponibleSinInvocarElRespaldo() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        RespuestaIa respuesta = respuesta();
        given(principal.responder("instrucciones")).willReturn(respuesta);
        assertThat(new AsistenteIa(List.of(principal, respaldo)).responder("instrucciones")).isSameAs(respuesta);
        verifyNoInteractions(respaldo);
    }

    @Test
    void intentaRespaldoSiElPrincipalNoEstaDisponible() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        given(principal.responder("prompt")).willThrow(new ProveedorNoDisponibleException(false));
        given(respaldo.responder("prompt")).willReturn(respuesta());
        assertThat(new AsistenteIa(List.of(principal, respaldo)).responder("prompt").texto())
                .isEqualTo("{\"mensaje\":\"Hola Sara\"}");
    }

    @Test
    void todosLosProveedoresNoDisponiblesProducen503() {
        ProveedorIa proveedor = mock(ProveedorIa.class);
        given(proveedor.responder("prompt")).willThrow(new ProveedorNoDisponibleException(true));
        assertThatThrownBy(() -> new AsistenteIa(List.of(proveedor)).responder("prompt"))
                .isInstanceOf(IaNoDisponibleException.class);
    }

    @Test
    void respuestaRecibidaPeroInvalidaNoDisparaOtraLlamadaConCosto() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        given(principal.responder("prompt")).willThrow(new RespuestaIaInvalidaException());
        assertThatThrownBy(() -> new AsistenteIa(List.of(principal, respaldo)).responder("prompt"))
                .isInstanceOf(RespuestaIaInvalidaException.class);
        verifyNoInteractions(respaldo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es JSON", "{}", "{\"mensaje\":4}", "[]", "{\"mensaje\":\" \"}",
            "{\"mensaje\":\"Hola\"} {}", "```json\n{\"mensaje\":\"Hola\"}\n```"})
    void lectorRechazaJsonNoUtilizable(String texto) {
        LectorJsonIa lector = new LectorJsonIa(new ObjectMapper());
        assertThatThrownBy(() -> lector.mensaje(texto)).isInstanceOf(RespuestaIaInvalidaException.class);
    }

    @Test
    void lectorAcepta700CaracteresPeroNo701() {
        LectorJsonIa lector = new LectorJsonIa(new ObjectMapper());
        assertThat(lector.mensaje("{\"mensaje\":\"" + "a".repeat(700) + "\"}")).hasSize(700);
        assertThatThrownBy(() -> lector.mensaje("{\"mensaje\":\"" + "a".repeat(701) + "\"}"))
                .isInstanceOf(RespuestaIaInvalidaException.class);
    }

    @Test
    void promptIncluyeLasVariablesYLasInstruccionesDelPdfSinReinterpretarLosDatos() {
        String prompt = new BibliotecaPrompts().recordatorio("Salon {{cliente}}", "Sara",
                "Hora: 10:30 a. m.\nValor: $35.000");
        assertThat(prompt).contains("Salon {{cliente}}", "Sara", "10:30 a. m.", "$35.000",
                "\"mensaje\"", "WhatsApp", "SÍ", "700");
        assertThat(prompt).doesNotContain("{{salon}}", "{{datos}}");
    }

    private static RespuestaIa respuesta() {
        return new RespuestaIa("{\"mensaje\":\"Hola Sara\"}",
                new FacturaIa("Gemini", "simulado", 10, 5, BigDecimal.ZERO, 1));
    }
}

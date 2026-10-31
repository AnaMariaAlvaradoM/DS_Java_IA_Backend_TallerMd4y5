package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devsenior.sillon.cita.RecordatorioIaRespuesta;

import tools.jackson.databind.json.JsonMapper;

class LectorJsonIATest {

    private LectorJsonIA lectorJsonIA;

    @BeforeEach
    void setUp() {
        lectorJsonIA = new LectorJsonIA(new JsonMapper());
    }

    private RespuestaIA respuestaConTexto(String texto) {
        return new RespuestaIA(texto, "openai", "modelo", 1, 1, BigDecimal.ZERO, 10L);
    }

    @Test
    void unTextoSinLlavesDaRespuestaInvalida() {
        assertThatThrownBy(() -> lectorJsonIA.leer(respuestaConTexto("no hay json aqui"),
                RecordatorioIaRespuesta.class))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    void unJsonMalFormadoDaRespuestaInvalida() {
        assertThatThrownBy(() -> lectorJsonIA.leer(respuestaConTexto("{ mensaje: sin comillas }"),
                RecordatorioIaRespuesta.class))
                .isInstanceOf(RespuestaIAInvalidaException.class);
    }

    @Test
    void unJsonValidoSeDeserializaCorrectamente() {
        RecordatorioIaRespuesta resultado = lectorJsonIA.leer(
                respuestaConTexto("Aqui va: {\"mensaje\":\"Hola\"} gracias"), RecordatorioIaRespuesta.class);

        assertThat(resultado.mensaje()).isEqualTo("Hola");
    }
}

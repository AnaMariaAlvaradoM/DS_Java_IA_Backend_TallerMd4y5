package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

class ProveedorHttpIaTest {

    @Test
    void geminiUsaFormatoOpenAiUrlConfigurableYFacturaConTokensReales() {
        var builder = RestClient.builder().baseUrl("http://ia.simulada/base");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://ia.simulada/base/chat/completions"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer llave-simulada"))
                .andExpect(content().json("""
                        {"model":"modelo-simulado","response_format":{"type":"json_object"},
                        "messages":[{"role":"system","content":"instrucciones verificadas"},
                                    {"role":"user","content":"Redacte el borrador solicitado."}]}
                        """))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"{\\"mensaje\\":\\"Hola Sara a las 10:30\\"}"}}],
                         "usage":{"prompt_tokens":100,"completion_tokens":50}}
                        """, MediaType.APPLICATION_JSON));
        RespuestaIa respuesta = proveedor(false, builder).responder("instrucciones verificadas");
        assertThat(respuesta.texto()).contains("Sara", "10:30");
        assertThat(respuesta.factura()).extracting(FacturaIa::tokensEntrada, FacturaIa::tokensSalida,
                FacturaIa::proveedor, FacturaIa::modelo).containsExactly(100L, 50L, "Gemini", "modelo-simulado");
        assertThat(respuesta.factura().costoEstimadoUsd()).isEqualByComparingTo("0.0004");
        assertThat(respuesta.factura().milisegundos()).isGreaterThanOrEqualTo(0);
        servidor.verify();
    }

    @Test
    void respaldoUsaElFormatoClaudeSinSdk() {
        var builder = RestClient.builder().baseUrl("http://ia.simulada/v1");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://ia.simulada/v1/messages"))
                .andExpect(header("x-api-key", "llave-simulada"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(content().json("""
                        {"model":"modelo-simulado","max_tokens":1024,"system":"prompt",
                        "messages":[{"role":"user","content":"Redacte el borrador solicitado."}]}
                        """))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"{\\"mensaje\\":\\"Hola Sara\\"}"}],
                         "usage":{"input_tokens":100,"output_tokens":50}}
                        """, MediaType.APPLICATION_JSON));
        assertThat(proveedor(true, builder).responder("prompt").factura().proveedor()).isEqualTo("Claude");
        servidor.verify();
    }

    @ParameterizedTest
    @CsvSource({"401,true", "403,true", "429,false", "503,false"})
    void clasificaConfiguracionFrenteAFallosTemporales(int estado, boolean configuracion) {
        var builder = RestClient.builder().baseUrl("http://ia.simulada");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://ia.simulada/chat/completions"))
                .andRespond(withStatus(HttpStatusCode.valueOf(estado)).body("detalle-privado-del-proveedor"));
        assertThatExceptionOfType(ProveedorNoDisponibleException.class)
                .isThrownBy(() -> proveedor(false, builder).responder("prompt"))
                .satisfies(ex -> assertThat(ex.esConfiguracion()).isEqualTo(configuracion))
                .withMessageNotContaining("detalle-privado");
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-json", "{}", "{\"choices\":[]}", """
            {"choices":[{"message":{"content":"texto"}}],"usage":{"prompt_tokens":-1,"completion_tokens":3}}
            """})
    void respuestaHttpNoUtilizableSeDistingueDeProveedorNoDisponible(String cuerpo) {
        var builder = RestClient.builder().baseUrl("http://ia.simulada");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://ia.simulada/chat/completions"))
                .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON));
        assertThatExceptionOfType(RespuestaIaInvalidaException.class)
                .isThrownBy(() -> proveedor(false, builder).responder("prompt"));
        servidor.verify();
    }

    @Test
    void respaldoSinLlaveFallaComoConfiguracionAntesDeHacerHttp() {
        var builder = RestClient.builder();
        var servidor = MockRestServiceServer.bindTo(builder).build();
        var config = new ConfiguracionIa.Proveedor("http://ia.simulada", "modelo", "",
                BigDecimal.ONE, BigDecimal.TEN);
        assertThatExceptionOfType(ProveedorNoDisponibleException.class)
                .isThrownBy(() -> new ProveedorHttpIa("Claude", true, config, builder.build(), new ObjectMapper())
                        .responder("prompt"))
                .satisfies(ex -> assertThat(ex.esConfiguracion()).isTrue());
        servidor.verify();
    }

    private ProveedorHttpIa proveedor(boolean claude, RestClient.Builder builder) {
        var config = new ConfiguracionIa.Proveedor("http://ia.simulada", "modelo-simulado", "llave-simulada",
                new BigDecimal("2"), new BigDecimal("4"));
        return new ProveedorHttpIa(claude ? "Claude" : "Gemini", claude, config, builder.build(), new ObjectMapper());
    }
}

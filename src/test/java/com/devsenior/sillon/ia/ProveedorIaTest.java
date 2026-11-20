package com.devsenior.sillon.ia;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

class ProveedorIaTest {
    final RestClient.Builder builder = RestClient.builder();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    ProveedorIa proveedor(String clave, String respaldo) throws Exception {
        return new ProveedorIa(
                builder.build(),
                JsonMapper.builder().build(),
                "http://gemini.test",
                clave,
                "flash",
                "http://claude.test",
                respaldo,
                "haiku",
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.TEN);
    }

    @Test
    void usaClaudeTrasErrorHttpYConservaConsumoReal() throws Exception {
        var ia = proveedor("prueba", "respaldo");
        server.expect(requestTo("http://gemini.test/chat/completions"))
                .andRespond(withServerError());
        server.expect(requestTo("http://claude.test/v1/messages"))
                .andRespond(
                        withSuccess(
                                "{\"content\":[{\"text\":\"{}\"}],\"usage\":{\"input_tokens\":5,\"output_tokens\":2}}",
                                APPLICATION_JSON));
        var factura = ia.consultar("Sistema", "Hola").factura();
        assertThat(factura.proveedor()).isEqualTo("Claude");
        assertThat(factura.tokensEntrada()).isEqualTo(5);
        assertThat(factura.costoEstimadoUsd()).isEqualByComparingTo("0.000025");
        server.verify();
    }

    @Test
    void ambosProveedoresAusentesDan503() throws Exception {
        var ia = proveedor("", "");
        assertThatThrownBy(() -> ia.consultar("Sistema", "Hola"))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
        server.verify();
    }

    @Test
    void lectorRechazaRespuestaQueNoSeaObjeto() throws Exception {
        var ia = proveedor("", "");
        for (var invalido : new String[] {"[]", "null", "texto"}) {
            assertThatThrownBy(() -> ia.leer(invalido)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test
    void plantillasIncluyenDatosDelContrato() throws Exception {
        var ia = proveedor("", "");
        assertThat(ia.promptRecordatorio()).contains("{salon}", "{cliente}", "{datos}", "mensaje");
        assertThat(ia.promptChat())
                .contains("{catalogo}", "{direccion}", "{horario}", "respuesta", "escalar");
    }
}

package com.devsenior.sillon.pago;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class WebhookTest {
    final Clock clock = Clock.fixed(Instant.ofEpochSecond(2000), ZoneOffset.UTC);
    final byte[] cuerpo = "{\"id\":\"evt_demo\"}".getBytes(StandardCharsets.UTF_8);
    final String secreto = "solo-pruebas";

    String cabecera(long t) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String texto = t + "." + new String(cuerpo, StandardCharsets.UTF_8);
        return "t="
                + t
                + ",v1="
                + HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(longs = {1700, 1999, 2000, 2300})
    void aceptaFirmaVigente(long t) throws Exception {
        FirmaStripe.verificar(cabecera(t), cuerpo, secreto, clock);
    }

    @ParameterizedTest
    @ValueSource(longs = {1699, 2301})
    void exigeVentanaDeCincoMinutos(long t) throws Exception {
        String header = cabecera(t);
        assertThatThrownBy(() -> FirmaStripe.verificar(header, cuerpo, secreto, clock))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void admiteRotacionDeFirmas() throws Exception {
        FirmaStripe.verificar(cabecera(2000) + ",v1=0000", cuerpo, secreto, clock);
    }

    @Test
    void detectaCuerpoDistinto() throws Exception {
        String header = cabecera(2000);
        assertThatThrownBy(() -> FirmaStripe.verificar(header, new byte[0], secreto, clock))
                .isInstanceOf(ResponseStatusException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"v1=0000", "t=hola,v1=x", "t=2000"})
    void rechazaCabecerasIncompletas(String header) {
        assertThatThrownBy(() -> FirmaStripe.verificar(header, cuerpo, secreto, clock))
                .isInstanceOf(ResponseStatusException.class);
    }
}

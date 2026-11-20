package com.devsenior.sillon.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.devsenior.sillon.compartido.Entradas;
import com.devsenior.sillon.ia.*;
import com.devsenior.sillon.seguridad.Actor;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.util.List;

class AsistenteServicioTest {
    final IaDatos datos = mock(IaDatos.class);
    final ProveedorIa ia = mock(ProveedorIa.class);
    final Asistente servicio = new Asistente(null, datos, ia);
    final Actor dueno = new Actor(1, 2, "DUENO", "due@example.test");
    final Entradas.Chat chat =
            new Entradas.Chat("Ana", "3105557890", List.of(new Entradas.Turno("CLIENTE", "Hola")));

    void prepararChat(String content) {
        when(datos.paraChat("luz"))
                .thenReturn(
                        new IaDatos.ContextoChat(2, "Luz", "Calle 3", "08–19", "Corte de 45 min"));
        when(ia.promptChat()).thenReturn("{salon} {direccion} {horario} {catalogo} {cliente}");
        when(ia.consultar(anyString(), anyString()))
                .thenReturn(new ProveedorIa.Resultado(content, null));
        when(ia.leer(content)).thenReturn(JsonMapper.builder().build().readTree(content));
    }

    void prepararRecordatorio(String content) {
        when(datos.paraCita(dueno, 8))
                .thenReturn(new IaDatos.Contexto(2, "Luz", "Ana", "3105557890", "Hora: 10:30"));
        when(ia.promptRecordatorio()).thenReturn("{salon} {cliente} {datos}");
        when(ia.consultar(anyString(), anyString()))
                .thenReturn(new ProveedorIa.Resultado(content, null));
        when(ia.leer(content)).thenReturn(JsonMapper.builder().build().readTree(content));
        when(datos.consumir(2)).thenReturn(9);
    }

    @Test
    void sinCreditosEvitaLlamarAlProveedor() {
        when(datos.paraChat("luz"))
                .thenThrow(
                        new ResponseStatusException(
                                org.springframework.http.HttpStatus.PAYMENT_REQUIRED));
        assertThatThrownBy(() -> servicio.chat("luz", chat))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(ia);
    }

    @Test
    void consultaInformativaConsumeUnCredito() {
        prepararChat("{\"respuesta\":\"Corte de 45 minutos\",\"escalar\":false}");
        var r = servicio.chat("luz", chat);
        assertThat(r.solicitudId()).isNull();
        assertThat(r.escalado()).isFalse();
        verify(datos).consumir(2);
        verify(datos, never()).solicitar(anyLong(), any(), anyString());
    }

    @Test
    void derivaLaSolicitudConSuMotivo() {
        prepararChat("{\"respuesta\":\"Le contactaremos\",\"escalar\":true}");
        when(datos.solicitar(2, chat, "PEDIDO_POR_IA")).thenReturn(9L);
        assertThat(servicio.chat("luz", chat).solicitudId()).isEqualTo(9L);
    }

    @Test
    void bloqueoComercialPrevalece() {
        prepararChat("{\"respuesta\":\"Hay reembolso inmediato\",\"escalar\":false}");
        var resultado = servicio.chat("luz", chat);
        assertThat(resultado.respuesta()).isEqualTo(Asistente.MENSAJE_SEGURO);
        verify(datos).solicitar(2, chat, "REGLA_DE_SEGURIDAD");
    }

    @Test
    void devuelveBorradorConTelefonoYSaldo() {
        prepararRecordatorio("{\"mensaje\":\"Le esperamos a las 10:30\"}");
        var r = servicio.recordar(dueno, 8);
        assertThat(r.telefonoCliente()).isEqualTo("3105557890");
        assertThat(r.creditosRestantes()).isEqualTo(9);
        assertThat(r.citaId()).isEqualTo(8);
    }

    @Test
    void respuestaSinMensajeConsumeElCreditoYaFacturado() {
        prepararRecordatorio("{}");
        assertThatThrownBy(() -> servicio.recordar(dueno, 8))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
        verify(datos).consumir(2);
    }
}

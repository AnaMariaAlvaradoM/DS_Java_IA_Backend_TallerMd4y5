package com.devsenior.sillon.cita;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.devsenior.sillon.cita.dto.DatosRecordatorio;
import com.devsenior.sillon.ia.AsistenteIa;
import com.devsenior.sillon.ia.BibliotecaPrompts;
import com.devsenior.sillon.ia.ConfiguracionIa;
import com.devsenior.sillon.ia.LectorJsonIa;
import com.devsenior.sillon.ia.ProveedorHttpIa;
import com.devsenior.sillon.salon.CreditosIaService;

import tools.jackson.databind.ObjectMapper;

class RecordatorioProveedorTest {

    @Test
    void aceptaElJsonDelSimuladorQueOmiteElNombreDelCliente() {
        var mapper = new ObjectMapper();
        var citas = mock(CitaService.class);
        var creditos = mock(CreditosIaService.class);
        given(citas.datosParaRecordatorio(1L, 2L)).willReturn(new DatosRecordatorio(2L, "Sillón",
                "Andrés Gómez", "3102223344", """
                Servicio: Corte clásico (45 minutos)
                Estilista: Sara Mejía
                Fecha: viernes 20 de noviembre
                Hora: 10:30 a. m.
                Valor: $35.000
                Dirección: Calle 10 # 43-20, Medellín
                """, "10:30"));
        given(creditos.consumir(1L)).willReturn(9);
        String mensaje = "Hola. Le recordamos su cita. Servicio: Corte clásico (45 minutos). "
                + "Estilista: Sara Mejía. Fecha: viernes 20 de noviembre. Hora: 10:30 a. m.. "
                + "Valor: $35.000. Dirección: Calle 10 # 43-20, Medellín. "
                + "¿Nos confirma su asistencia respondiendo SÍ?";
        String contenido = mapper.writeValueAsString(Map.of("mensaje", mensaje));
        String respuestaHttp = mapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("role", "assistant", "content", contenido))),
                "usage", Map.of("prompt_tokens", 100, "completion_tokens", 60)));
        var builder = RestClient.builder().baseUrl("http://ia.simulada");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("http://ia.simulada/chat/completions"))
                .andRespond(withSuccess(respuestaHttp, MediaType.APPLICATION_JSON));
        var configuracion = new ConfiguracionIa.Proveedor("http://ia.simulada", "simulado", "llave-simulada",
                BigDecimal.ZERO, BigDecimal.ZERO);
        var proveedor = new ProveedorHttpIa("Gemini", false, configuracion, builder.build(), mapper);
        var servicio = new RecordatorioService(citas, creditos, new BibliotecaPrompts(),
                new AsistenteIa(List.of(proveedor)), new LectorJsonIa(mapper));

        assertThat(new LectorJsonIa(mapper).mensaje(contenido)).isEqualTo(mensaje)
                .contains("Sara", "10:30").doesNotContain("Andrés Gómez");
        var respuesta = servicio.generar(1L, 2L);
        assertThat(respuesta.mensaje()).isEqualTo(mensaje);
        assertThat(respuesta.telefonoCliente()).isEqualTo("3102223344");
        assertThat(respuesta.creditosRestantes()).isEqualTo(9);
        assertThat(respuesta.factura().tokensEntrada()).isEqualTo(100);
        verify(creditos).verificar(1L);
        verify(creditos).consumir(1L);
        servidor.verify();
    }
}

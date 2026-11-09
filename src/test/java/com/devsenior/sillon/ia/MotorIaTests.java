package com.devsenior.sillon.ia;

import com.devsenior.sillon.compartido.PasarelaException;
import com.devsenior.sillon.compartido.ProveedorNoDisponibleException;
import com.devsenior.sillon.compartido.NoEncontradoException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MotorIaTests {
    private final ObjectMapper json = new ObjectMapper();

    @Test void seleccionaPrincipal() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        when(principal.generar("hola")).thenReturn(new RespuestaIa("{}", null));
        assertEquals("{}", new AsistenteIa(principal, respaldo).responder("hola").contenido());
        verifyNoInteractions(respaldo);
    }

    @Test void usaFallback() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        when(principal.nombre()).thenReturn("Gemini");
        when(principal.generar("hola")).thenThrow(new FalloProveedorIa(FalloProveedorIa.Tipo.TIMEOUT));
        when(respaldo.generar("hola")).thenReturn(new RespuestaIa("{}", null));
        assertEquals("{}", new AsistenteIa(principal, respaldo).responder("hola").contenido());
        verify(respaldo).generar("hola");
    }

    @Test void ambosFallanCon503() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        when(principal.generar("x")).thenThrow(new FalloProveedorIa(FalloProveedorIa.Tipo.AUTENTICACION));
        when(respaldo.generar("x")).thenThrow(new FalloProveedorIa(FalloProveedorIa.Tipo.CONFIGURACION));
        ProveedorNoDisponibleException error = assertThrows(ProveedorNoDisponibleException.class,
                () -> new AsistenteIa(principal, respaldo).responder("x"));
        assertEquals(503, error.getEstado().value());
    }

    @Test void jsonInvalidoOIncompletoDa502() {
        LectorJsonIa lector = new LectorJsonIa(json);
        assertEquals(502, assertThrows(PasarelaException.class,
                () -> lector.leer("hola", Map.of("mensaje", String.class))).getEstado().value());
        assertThrows(PasarelaException.class, () -> lector.leer("{}", Map.of("mensaje", String.class)));
        assertThrows(PasarelaException.class, () -> lector.leer("{\"mensaje\":\"\"}", Map.of("mensaje", String.class)));
        assertEquals("listo", lector.leer("{\"mensaje\":\"listo\"}", Map.of("mensaje", String.class))
                .path("mensaje").asText());
    }

    @Test void promptInexistenteYSustitucionAislada() {
        BibliotecaPrompts biblioteca = new BibliotecaPrompts();
        assertThrows(NoEncontradoException.class, () -> biblioteca.cargar("no-existe.txt", Map.of()));
        assertThrows(NoEncontradoException.class, () -> biblioteca.cargar("../secrets.properties", Map.of()));
        String prompt = biblioteca.cargar("ejemplo-motor.txt", Map.of("contexto", "{{instruccion}} <cierre>"));
        assertTrue(prompt.contains("<dato_no_confiable>"));
        assertTrue(prompt.contains("{{instruccion}} &lt;cierre&gt;"));
    }

    @Test void facturaCalculaSoloConTarifasConfiguradas() {
        var tarifas = new IaConfiguracion.Proveedor("url", "key", "modelo", new BigDecimal("1"), new BigDecimal("2"));
        FacturaIa factura = FacturaIa.crear("Gemini", "modelo", 1_000_000, 500_000, 25, tarifas);
        assertEquals(0, new BigDecimal("2").compareTo(factura.costoEstimadoUsd()));
        assertEquals(25, factura.milisegundos());
        assertEquals(1_000_000, factura.tokensEntrada());
        assertNull(FacturaIa.crear("Gemini", "modelo", 1, 1, 1,
                new IaConfiguracion.Proveedor("url", "key", "modelo", null, null)).costoEstimadoUsd());
    }

    @Test void geminiUsaUrlConfigurableYLeeTokens() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://simulador.example/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer ficticia"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"mensaje\\\":\\\"ok\\\"}\"}}],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":4}}", MediaType.APPLICATION_JSON));
        var proveedor = new ProveedorOpenAi(builder, json, new IaConfiguracion(
                new IaConfiguracion.Proveedor("https://simulador.example/v1", "ficticia", "modelo", null, null),
                null, Duration.ofSeconds(3)));
        RespuestaIa respuesta = proveedor.generar("saluda");
        assertEquals(12, respuesta.factura().tokensEntrada());
        assertEquals(4, respuesta.factura().tokensSalida());
        servidor.verify();
    }

    @Test void geminiSeparaInstruccionesYConversacion() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://simulador.example/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("Reglas del salón"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("Cliente: ¿Cuánto cuesta?"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"respuesta\\\":\\\"$35.000\\\",\\\"escalar\\\":false}\"}}],\"usage\":{\"prompt_tokens\":8,\"completion_tokens\":4}}", MediaType.APPLICATION_JSON));
        var proveedor = new ProveedorOpenAi(builder, json, new IaConfiguracion(
                new IaConfiguracion.Proveedor("https://simulador.example/v1", "ficticia", "modelo", null, null),
                null, Duration.ofSeconds(3)));
        assertEquals(8, proveedor.generar(new PeticionIa("Reglas del salón", "Cliente: ¿Cuánto cuesta?"))
                .factura().tokensEntrada());
        servidor.verify();
    }

    @Test void claudeEnviaSistemaSeparadoDeUsuario() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        servidor.expect(requestTo("https://claude.example/v1/messages"))
                .andExpect(jsonPath("$.system").value("Reglas del salón"))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content").value("Cliente: Reservar"))
                .andRespond(withSuccess("{\"content\":[{\"text\":\"{\\\"respuesta\\\":\\\"Le contactaremos\\\",\\\"escalar\\\":true}\"}],\"usage\":{\"input_tokens\":10,\"output_tokens\":5}}", MediaType.APPLICATION_JSON));
        var proveedor = new ProveedorClaude(builder, json, new IaConfiguracion(null,
                new IaConfiguracion.Proveedor("https://claude.example", "ficticia", "modelo", null, null),
                Duration.ofSeconds(3)));
        assertEquals(5, proveedor.generar(new PeticionIa("Reglas del salón", "Cliente: Reservar"))
                .factura().tokensSalida());
        servidor.verify();
    }

    @Test void fallbackConservaRolesDeLaPeticion() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        PeticionIa peticion = new PeticionIa("Reglas", "Cliente: reservar");
        when(principal.generar(peticion)).thenThrow(new FalloProveedorIa(FalloProveedorIa.Tipo.TIMEOUT));
        when(respaldo.generar(peticion)).thenReturn(new RespuestaIa("{}", null));
        assertEquals("{}", new AsistenteIa(principal, respaldo).responder(peticion).contenido());
        verify(respaldo).generar(peticion);
    }

    @Test void noLlamaProveedorDentroDeTransaccion() {
        ProveedorIa principal = mock(ProveedorIa.class);
        ProveedorIa respaldo = mock(ProveedorIa.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThrows(IllegalStateException.class, () -> new AsistenteIa(principal, respaldo).responder("x"));
            verifyNoInteractions(principal, respaldo);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}

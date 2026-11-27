package com.devsenior.sillon;

import com.devsenior.sillon.auth.dto.RegistroRequest;
import com.devsenior.sillon.cita.dto.RecordatorioResponse;
import com.devsenior.sillon.ia.RespuestaIA;
import com.devsenior.sillon.publico.dto.DisponibilidadResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContratoJsonTest {
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void registroAceptaElNombreDeSalonEnCamelCase() throws Exception {
        RegistroRequest request = objectMapper.readValue("""
                {
                  "salonNombre": "Salon Centro",
                  "nombre": "Ana",
                  "email": "ana@example.com",
                  "ciudad": "Bogota",
                  "password": "contrasena123",
                  "telefono": "3001234567",
                  "direccion": "Calle 1"
                }
                """, RegistroRequest.class);

        assertEquals("Salon Centro", request.salonNombre());
    }

    @Test
    void disponibilidadExponeLosDosNombresDeListaCompatibles() {
        DisponibilidadResponse response = new DisponibilidadResponse(
                LocalDate.of(2026, 11, 27), 10L, 20L, 30, List.of(LocalTime.of(10, 0)));

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals("10:00", json.get("horariosLibres").get(0).asText());
        assertEquals("10:00", json.get("horariosDisponibles").get(0).asText());
    }

    @Test
    void recordatorioExponeElTelefonoYProveedorEnAmbosContratos() {
        RespuestaIA factura = new RespuestaIA("{}", "mock", "modelo-test", 2, 1, BigDecimal.ZERO, 3L);
        RecordatorioResponse response = new RecordatorioResponse(
                1L, "3102223344", "Recordatorio para Sara", 9, factura);

        JsonNode json = objectMapper.valueToTree(response);

        assertEquals("3102223344", json.get("telefono").asText());
        assertEquals("3102223344", json.get("telefonoCliente").asText());
        assertEquals("mock", json.get("proveedor").asText());
        assertEquals("mock", json.get("factura").get("proveedor").asText());
    }
}

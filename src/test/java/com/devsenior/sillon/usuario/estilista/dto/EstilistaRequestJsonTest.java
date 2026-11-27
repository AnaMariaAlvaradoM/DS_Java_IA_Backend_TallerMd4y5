package com.devsenior.sillon.usuario.estilista.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstilistaRequestJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aceptaNombresHistoricosDeListaDeServicios() throws Exception {
        for (String campo : List.of("servicioIds", "serviciosId", "serviciosIds")) {
            EstilistaRequest solicitud = objectMapper.readValue("""
                    {
                      "nombre": "Mateo Gil",
                      "email": "mateo@example.com",
                      "password": "mateo12345",
                      "%s": [12, 34]
                    }
                    """.formatted(campo), EstilistaRequest.class);

            assertEquals(List.of(12L, 34L), solicitud.servicioIds());
        }
    }

    @Test
    void responseSerializaLaListaConElNombreServicios() throws Exception {
        EstilistaResponse response = new EstilistaResponse(
                9L, "Mateo Gil", "mateo@example.com", true, List.of("Corte", "Barba"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals(List.of("Corte", "Barba"),
                objectMapper.convertValue(json.get("servicios"), objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, String.class)));
    }
}

package com.devsenior.sillon.ia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.devsenior.sillon.ia.excepcion.ProveedorIAException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProveedorIAExceptionTest {

    @ParameterizedTest(name = "HTTP {0} → configuración: {1}")
    @CsvSource({
            "400, true",
            "401, true",
            "403, true",
            "404, true",
            "429, false",
            "500, false",
            "503, false",
            "529, false"
    })
    void desdeCodigoHttp_clasificaConfiguracionOTemporal(int codigo, boolean esConfiguracion) {
        assertEquals(esConfiguracion, ProveedorIAException.desdeCodigoHttp("openai", codigo).isErrorDeConfiguracion());
    }
}

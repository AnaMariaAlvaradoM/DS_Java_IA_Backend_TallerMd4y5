package com.devsenior.sillon.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProveedorIAExceptionTest {

    @ParameterizedTest(name = "HTTP {0} -> error de configuración: {1}")
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
    void desdeEstadoHttp_clasificaElError(int estadoHttp, boolean esDeConfiguracion) {
        ProveedorIAException ex = ProveedorIAException.desdeEstadoHttp("openai", estadoHttp);

        assertEquals(esDeConfiguracion, ex.isErrorDeConfiguracion());
    }
}

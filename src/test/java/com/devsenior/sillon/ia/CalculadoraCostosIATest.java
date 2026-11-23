
package com.devsenior.sillon.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalculadoraCostosIATest {

    private final CalculadoraCostosIA calculadora =
            new CalculadoraCostosIA();

    @Test
    void calcularCostoCorrectamente() {

        double costo = calculadora.calcular(
                1000,
                500,
                0.30,
                2.50
        );

        assertEquals(0.00155, costo, 0.00000001);
    }

    @Test
    void calcularCostoConCeroTokens() {

        double costo = calculadora.calcular(
                0,
                0,
                0.30,
                2.50
        );

        assertEquals(0.0, costo);
    }

    @Test
    void rechazarTokensNegativos() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcular(
                        -100,
                        500,
                        0.30,
                        2.50
                )
        );
    }

    @Test
    void rechazarTarifasNegativas() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcular(
                        1000,
                        500,
                        -0.30,
                        2.50
                )
        );
    }

    @Test
    void rechazarTarifasInfinitas() {

        assertThrows(
                IllegalArgumentException.class,
                () -> calculadora.calcular(
                        1000,
                        500,
                        Double.POSITIVE_INFINITY,
                        2.50
                )
        );
    }
}

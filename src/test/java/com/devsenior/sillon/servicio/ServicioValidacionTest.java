package com.devsenior.sillon.servicio;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ServicioValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Servicio con duración menor a 15 minutos falla validación (400)")
    void servicio_duracionMenor15_fallaValidacion() {
        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion(
                "Corte express",
                null,
                5, // Menor a 15 minutos
                35000,
                true);

        Set<ConstraintViolation<ServicioCreacionPeticion>> violaciones = validator.validate(peticion);
        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("duracionMinutos")));
    }

    @Test
    @DisplayName("Servicio con precio negativo o cero falla validación (400)")
    void servicio_precioNegativo_fallaValidacion() {
        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion(
                "Corte gratis",
                null,
                30,
                -100, // Menor a 1
                true);

        Set<ConstraintViolation<ServicioCreacionPeticion>> violaciones = validator.validate(peticion);
        assertFalse(violaciones.isEmpty());
        assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("precio")));
    }

    @Test
    @DisplayName("Servicio con datos válidos pasa validación")
    void servicio_datosValidos_pasaValidacion() {
        ServicioCreacionPeticion peticion = new ServicioCreacionPeticion(
                "Corte clásico",
                "Con lavado",
                45,
                35000,
                true);

        Set<ConstraintViolation<ServicioCreacionPeticion>> violaciones = validator.validate(peticion);
        assertTrue(violaciones.isEmpty());
    }
}

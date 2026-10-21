package com.devsenior.sillon.seguridad;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class LongitudBcryptValidator implements ConstraintValidator<ContrasenaBcrypt, String> {
    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}

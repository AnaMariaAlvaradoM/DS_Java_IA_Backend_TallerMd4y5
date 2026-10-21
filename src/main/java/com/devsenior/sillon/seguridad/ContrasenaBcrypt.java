package com.devsenior.sillon.seguridad;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LongitudBcryptValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ContrasenaBcrypt {
    String message() default "no puede superar 72 bytes en UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

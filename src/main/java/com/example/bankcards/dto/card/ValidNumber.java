package com.example.bankcards.dto.card;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CardNumberValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNumber {
    String message() default "Card number requires 16 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

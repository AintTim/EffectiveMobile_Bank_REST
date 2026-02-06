package com.example.bankcards.dto.card;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CardNumberValidator implements ConstraintValidator<ValidNumber, String> {
    private static final Pattern CARD_REGEX = Pattern.compile("^\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return CARD_REGEX.matcher(value).matches();
    }
}

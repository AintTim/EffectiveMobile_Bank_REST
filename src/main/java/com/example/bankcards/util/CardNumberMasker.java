package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class CardNumberMasker {

    public String mask(String value) {
        return "**** **** **** " + value.substring(12);
    }
}

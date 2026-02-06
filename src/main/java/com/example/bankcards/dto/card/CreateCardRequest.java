package com.example.bankcards.dto.card;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCardRequest {

    @ValidNumber
    private String number;

    @NotNull(message = "User ID must be provided")
    private Long userId;

    @NotNull(message = "Expiration date must be provided")
    @FutureOrPresent
    private LocalDate expirationDate;
}
